/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.resource;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.FileSystemUtils;

@Slf4j
public class FileSystemResourceStoreCache implements DisposableBean {

    private final Path base;
    private final boolean disposable;
    private final @Getter FileSystemResourceStore localOnlyStore;
    private final Predicate<String> noClobber;

    private FileSystemResourceStoreCache(
            @NonNull Path cacheDirectory, boolean disposable, @NonNull Predicate<String> noClobber) {
        this.disposable = disposable;
        this.noClobber = noClobber;
        Preconditions.checkArgument(
                Files.isDirectory(cacheDirectory),
                "Cache directory is not a directory: %s",
                cacheDirectory.toAbsolutePath());
        Preconditions.checkArgument(
                Files.isWritable(cacheDirectory),
                "Cache directory is not writable: %s",
                cacheDirectory.toAbsolutePath());
        this.base = cacheDirectory;
        this.localOnlyStore = new FileSystemResourceStore(new File(this.base.toUri()));
    }

    @SneakyThrows
    public static @NonNull FileSystemResourceStoreCache newTempDirInstance(@NonNull Predicate<String> noClobber) {
        boolean disposable = true;
        Path tempDirectory = Files.createTempDirectory("pgconfig_resourcestore_cache");
        return new FileSystemResourceStoreCache(tempDirectory, disposable, noClobber);
    }

    @VisibleForTesting
    public static @NonNull FileSystemResourceStoreCache ofProvidedDirectory(
            @NonNull Path cacheDirectory, @NonNull Predicate<String> noClobber) {
        boolean disposable = false;
        return new FileSystemResourceStoreCache(cacheDirectory, disposable, noClobber);
    }

    public Path localPath(@NonNull String resourcePath) {
        return base.resolve(resourcePath);
    }

    @Override
    public void destroy() {
        if (disposable && Files.isDirectory(this.base)) {
            try {
                log.info("Deleting resource store cache directory {}", base);
                FileSystemUtils.deleteRecursively(base);
                log.info("Resource store cache directory {} deleted", base);
            } catch (IOException e) {
                log.warn("Error deleting resource cache {}", base, e);
            }
        }
    }

    @SneakyThrows
    public File getFile(PgconfigResource resource) {
        return dumpIfNeeded(resource).toFile();
    }

    /**
     * Whether the local copy of a file exists, and the rule of {@code needsDump} keeps it for a row of the given mtime.
     * A directory at the path of the copy is not a copy, whatever its mtime. A path refused by the disk, such as a name
     * too long for it, has no copy: the write of the copy then fails on its own.
     */
    public boolean isCurrent(@NonNull String resourcePath, long mtime) {
        BasicFileAttributes copy;
        // a single read, false on a failure as with Files.exists: a copy deleted between two reads would throw
        try {
            copy = Files.readAttributes(localPath(resourcePath), BasicFileAttributes.class);
        } catch (IOException _) {
            return false;
        }
        long copyMtime = copy.lastModifiedTime().toMillis();
        return copy.isRegularFile() && !needsDump(resourcePath, copyMtime, mtime);
    }

    /**
     * Ensures the local cache file for a resource exists and holds current content, dumping it from the database row
     * when needed.
     *
     * <p>A file with no local cache copy yet is always dumped: there is no risk of overwriting unsynced local work
     * because there was no local file to begin with.
     *
     * @return the path to the up to date local cache file
     */
    private Path dumpIfNeeded(PgconfigResource resource) {
        Preconditions.checkArgument(resource.isFile());
        if (!isCurrent(resource.path(), resource.lastmodified())) {
            dump(resource);
        }
        return toPath(resource);
    }

    /**
     * Decides whether an already cached local file must be refreshed from the database row.
     *
     * <p>For paths matching the no-clobber predicate, a local file newer than the database row is preserved:
     * gt-imagemosaic and similar consumers write directly to the cached file with raw file I/O, bypassing the
     * {@link Resource} API. A locally newer file therefore reflects work that the database does not know about yet. For
     * all other paths, any mismatch between the local file's modification time and the database row's triggers a
     * re-dump.
     */
    private boolean needsDump(String resourcePath, long fileMtime, long resourceMtime) {
        if (noClobber.test(resourcePath)) {
            return resourceMtime > fileMtime;
        }
        return fileMtime != resourceMtime;
    }

    @SneakyThrows
    public File getDirectory(PgconfigResource resource) {
        return ensureDirectory(resource).toFile();
    }

    @SneakyThrows
    public Path ensureDirectory(PgconfigResource resource) {
        Preconditions.checkArgument(resource.isDirectory());
        Path path = toPath(resource);
        return ensureDirectoryExists(path);
    }

    private Path ensureDirectoryExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        return path;
    }

    @SneakyThrows
    private Path dump(PgconfigResource resource) {
        try (InputStream in = resource.in()) {
            return dump(resource, in);
        }
    }

    @SneakyThrows
    public Path dump(PgconfigResource resource, InputStream in) {
        Preconditions.checkArgument(resource.isFile());
        return replaceCopy(toPath(resource), resource.lastmodified(), in);
    }

    /** Writes the local copy of a file, stamped with the mtime of its row. */
    public Path write(@NonNull String resourcePath, long mtime, @NonNull byte[] content) throws IOException {
        return replaceCopy(localPath(resourcePath), mtime, new ByteArrayInputStream(content));
    }

    /**
     * Replaces a local copy with a file written next to it and stamped with the mtime of its row. The move over the
     * copy is atomic: a reader of the previous copy keeps reading it, and no reader sees a copy half written.
     */
    private Path replaceCopy(Path copy, long mtime, InputStream content) throws IOException {
        Path directory = ensureDirectoryExists(copy.getParent());
        Path written = directory.resolve(".%s.tmp".formatted(UUID.randomUUID()));
        try {
            Files.copy(content, written);
            Files.setLastModifiedTime(written, FileTime.fromMillis(mtime));
            deleteEmptyDirectory(copy);
            moveOver(written, copy);
        } finally {
            Files.deleteIfExists(written);
        }
        return copy;
    }

    /**
     * Deletes an empty directory at the path of a copy: a directory resource replaced by a file of the same path can
     * leave one in the local copies. A directory with files is not deleted, and the copy fails.
     */
    private static void deleteEmptyDirectory(Path copy) throws IOException {
        if (Files.isDirectory(copy)) {
            Files.deleteIfExists(copy);
        }
    }

    // On a file system without atomic moves, a plain move replaces the copy, and a reader can miss it for a moment
    private static void moveOver(Path written, Path copy) throws IOException {
        try {
            Files.move(written, copy, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException _) {
            Files.move(written, copy, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void updateAll(List<Resource> list) {
        List<PgconfigResource> resources =
                list.stream().map(PgconfigResource.class::cast).toList();
        materialize(resources);
    }

    /**
     * Dumps files and creates directories for the given resources, honoring the no-clobber rule for locally newer
     * whitelisted files. Used to materialize DB-backed children of a directory.
     */
    @SneakyThrows
    public void materialize(List<PgconfigResource> resources) {
        for (PgconfigResource resource : resources) {
            if (resource.isDirectory()) {
                ensureDirectory(resource);
            } else if (resource.isFile()) {
                dumpIfNeeded(resource);
            }
        }
    }

    private Path toPath(PgconfigResource resource) {
        return localPath(resource.path());
    }

    /**
     * Removes the local cache copy (file or directory tree) of a deleted resource. Consumers like gt-imagemosaic read
     * the cache with raw file I/O; a stale copy left behind would keep resolving configuration the database no longer
     * holds, e.g. a coverage store re-created at the path of a deleted one picking up the old store's config files.
     */
    public void deleted(@NonNull String resourcePath) {
        Path path = localPath(resourcePath);
        try {
            FileSystemUtils.deleteRecursively(path);
        } catch (IOException e) {
            log.warn("Error deleting local cache copy of removed resource {}", resourcePath, e);
        }
    }

    @SneakyThrows
    public void moved(@NonNull PgconfigResource source, @NonNull PgconfigResource target) {
        Path sourcePath = toPath(source);
        if (Files.exists(sourcePath)) {
            Path targetPath = toPath(target);
            ensureDirectoryExists(targetPath.getParent());
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
