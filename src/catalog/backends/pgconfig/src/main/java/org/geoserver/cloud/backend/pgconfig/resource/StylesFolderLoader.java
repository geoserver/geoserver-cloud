/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.LoggingTemplate;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource.Type;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

/**
 * Writes the local copies of the files of a styles folder with one query, when the copy of one of them is missing or
 * outdated. The SLD parser reads the local copy of a style file, and a GetCapabilities parses the styles. Without the
 * folder load, the first GetCapabilities on a new pod reads each style file with a query of its own.
 *
 * <p>A styles folder is the {@code styles} directory at the root or in a workspace. A load writes the files of the
 * folder with a missing or outdated copy, and leaves out the files of its subfolders. It reads their content from the
 * database, and does not fill the content cache in memory. A copy that cannot be written fails alone, on the read of
 * its own file.
 *
 * <p>The index holds the sizes of the files. If the files to write add up to more than the maximum, the load does not
 * run, and each caller writes the copy of its own file.
 *
 * <p>A load holds the content of its files in memory until it writes the last copy, because the driver reads the whole
 * result before it hands over the first row: up to the maximum, or about twice as much when the content arrives as
 * text. Loads of different folders run in parallel.
 *
 * <p>One load runs per folder at a time. A caller in a folder being loaded waits for the load, then checks its file
 * again. A change committed during a load can leave an outdated copy: its mtime then differs from the row in the index,
 * and the next read of the file writes it again.
 */
@Slf4j
class StylesFolderLoader {

    /** The default maximum of the content written by a load, in bytes. */
    static final long MAX_FOLDER_BYTES = 16L * 1024 * 1024;

    private static final Pattern STYLES_FOLDER = Pattern.compile("(workspaces/[^/]+/)?styles");

    private final LoggingTemplate template;
    private final ResourceStoreCache index;
    private final FileSystemResourceStoreCache localCopies;
    private final long maxBytes;

    /** One lock per styles folder. A pod sees one styles folder per workspace, and one at the root. */
    private final ConcurrentMap<String, ReentrantLock> folderLocks = new ConcurrentHashMap<>();

    /**
     * @param template the template of the store, to read the content of the files
     * @param index the index of the store, with the sizes of the files
     * @param localCopies the local copies of the store
     * @param maxBytes above this size in total, the files of a folder are written one by one
     */
    public StylesFolderLoader(
            @NonNull JdbcTemplate template,
            @NonNull ResourceStoreCache index,
            @NonNull FileSystemResourceStoreCache localCopies,
            long maxBytes) {
        this.template = new LoggingTemplate(template).setLog(log);
        this.index = index;
        this.localCopies = localCopies;
        this.maxBytes = maxBytes;
    }

    /**
     * Writes the local copies of the folder of a file, if the file sits directly in a styles folder and its copy is
     * missing or outdated. Does nothing otherwise.
     */
    public void loadFolderOf(@NonNull ResourceRow file) {
        if (inStylesFolder(file) && !isCurrent(file)) {
            String folder = Paths.parent(file.path());
            loadOnce(folder, file, filesOf(folder));
        }
    }

    // Only the root has no parent, and the root is a directory
    private static boolean inStylesFolder(ResourceRow file) {
        return file.type() == Type.RESOURCE && isStylesFolder(Paths.parent(file.path()));
    }

    /** Whether a directory is a styles folder: {@code styles}, or {@code workspaces/<workspace>/styles}. */
    static boolean isStylesFolder(@NonNull String directoryPath) {
        return STYLES_FOLDER.matcher(directoryPath).matches();
    }

    private boolean isCurrent(ResourceRow file) {
        return localCopies.isCurrent(file.path(), file.mtime());
    }

    /** Returns the files of a folder, without the files of its subfolders. */
    private List<ResourceRow> filesOf(String folder) {
        List<ResourceRow> children = index.find(folder).map(index::children).orElseGet(List::of);
        return children.stream().filter(row -> row.type() == Type.RESOURCE).toList();
    }

    /**
     * Writes the copies of the files of a folder, unless the load of another caller wrote the copy of the file while
     * this one waited.
     *
     * <p>The caller reads the index before taking the lock. A read of the index can load it, and the load tells the
     * resource listeners of the changes found. A listener must not run under the lock: it may wait for a lock held by a
     * thread waiting for this folder, such as the lock of the notification dispatcher.
     */
    private void loadOnce(String folder, ResourceRow file, List<ResourceRow> folderFiles) {
        ReentrantLock lock = folderLocks.computeIfAbsent(folder, _ -> new ReentrantLock());
        lock.lock();
        try {
            if (!isCurrent(file)) {
                load(folder, folderFiles);
            }
        } finally {
            lock.unlock();
        }
    }

    private void load(String folder, List<ResourceRow> folderFiles) {
        List<ResourceRow> files =
                folderFiles.stream().filter(row -> !isCurrent(row)).toList();
        long bytes = files.stream().mapToLong(ResourceRow::size).sum();
        if (bytes > maxBytes) {
            log.debug("Not loading {}: the files to write add up to {} bytes, above {}", folder, bytes, maxBytes);
            return;
        }
        writeCopies(files);
        log.debug("Loaded {}: wrote the local copies of {} files", folder, files.size());
    }

    /**
     * Reads the content of the files with one query, and writes their copies, stamped with the mtimes of their rows.
     */
    private void writeCopies(List<ResourceRow> files) {
        if (files.isEmpty()) {
            return;
        }
        Map<Long, String> paths = files.stream().collect(Collectors.toMap(ResourceRow::id, ResourceRow::path));
        long[] ids = files.stream().mapToLong(ResourceRow::id).toArray();
        RowCallbackHandler writer = row -> writeCopy(row, paths);
        String sql =
                """
                SELECT id, mtime, content FROM resourcestore WHERE id = ANY(?)
                """;
        template.query(sql, writer, ids);
    }

    // A copy that cannot be written, such as one with a name too long for the disk, fails alone on the read of its file
    private void writeCopy(ResultSet row, Map<Long, String> paths) throws SQLException {
        String path = paths.get(row.getLong("id"));
        long mtime = PgconfigResourceRowMapper.mtime(row);
        byte[] content = row.getBytes("content");
        try {
            localCopies.write(path, mtime, content);
        } catch (IOException e) {
            log.warn("Could not write the local copy of {} with the files of its folder", path, e);
        }
    }
}
