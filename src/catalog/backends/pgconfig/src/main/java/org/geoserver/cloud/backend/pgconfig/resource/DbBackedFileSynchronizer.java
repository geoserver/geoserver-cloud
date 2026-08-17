/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;

/**
 * Service bean keeping db-backed files written with raw file I/O consistent between a pod's local resource cache and
 * the pgconfig resource store: {@link #sync pushes} local files matching the db-backed patterns into the database,
 * making them visible to every pod; {@link #materialize materializes} them back into a pod's local cache for consumers
 * that resolve paths with raw file I/O instead of the Resource API; and {@link #syncDeletions reconciles} local
 * deletions by removing the database rows of files a mutating operation deleted.
 *
 * <p>Nothing here is specific to a particular consumer: any code that writes files under a db-backed directory
 * bypassing the Resource API can integrate by invoking these operations at its read and mutation boundaries. The
 * driving case is gt-imagemosaic, which rewrites {@code <coverage>.properties} and {@code sample_image.dat} with raw
 * {@code java.io.File} I/O; {@link org.geoserver.cloud.backend.pgconfig.coverage.MosaicSyncingResourcePool} and its
 * reader decorators are its trigger points.
 *
 * <p>Only files matching the store's db-backed patterns are pushed, and only when the local copy is newer than the
 * database row.
 */
@Slf4j
public class DbBackedFileSynchronizer {

    /**
     * Minimum time between two database pulls for the same directory path while its local db-backed files are present.
     * Read paths trigger a pull per request (e.g. coverage reader acquisition on every WMS/WCS request); this throttle
     * caps the database lookup cost on cache hits. A path with no local db-backed files is always pulled regardless of
     * the throttle: the consumer about to read cannot work without them.
     */
    static final long MATERIALIZE_THROTTLE_MS = 10_000;

    /**
     * Minimum time between two read-path pushes of the same directory path. Without this throttle each request would
     * pay a directory scan even when nothing was written. Mutating operations call {@link #sync} directly and propagate
     * immediately.
     */
    static final long SYNC_THROTTLE_MS = 10_000;

    private final PgconfigResourceStore store;
    private final FileSystemResourceStoreCache cache;

    /**
     * Per directory path, the newest local file mtime covered by the last completed sync of that directory. A file
     * whose mtime is at or below this value was already dealt with by an earlier scan (pushed, or found not newer than
     * its database row) and is skipped without a database lookup; only strictly newer files are considered again. Holds
     * file mtimes, unlike the wall-clock stamps in {@link #lastMaterialized} and {@link #lastSynced}, and only advances
     * when a scan completes without error, letting a failed push retry on the next sync.
     */
    private final ConcurrentMap<String, Long> newestSyncedMtime = new ConcurrentHashMap<>();

    /**
     * Per directory path, the wall-clock time of the last database pull, backing the {@link #MATERIALIZE_THROTTLE_MS}
     * throttle in {@link #materialize}.
     */
    private final ConcurrentMap<String, Long> lastMaterialized = new ConcurrentHashMap<>();

    /**
     * Per directory path, the wall-clock time of the last completed push scan, backing the {@link #SYNC_THROTTLE_MS}
     * throttle in {@link #syncThrottled}.
     */
    private final ConcurrentMap<String, Long> lastSynced = new ConcurrentHashMap<>();

    /**
     * In-flight database pulls by directory path: concurrent {@link #materialize} callers for the same path wait on the
     * thread already pulling instead of racing the same file copies, which could expose a torn file to a raw-I/O
     * reader.
     */
    private final ConcurrentMap<String, CompletableFuture<Void>> inFlightPulls = new ConcurrentHashMap<>();

    public DbBackedFileSynchronizer(@NonNull PgconfigResourceStore store, @NonNull FileSystemResourceStoreCache cache) {
        this.store = store;
        this.cache = cache;
    }

    /**
     * Pushes every local file directly under the given resource store directory path that matches the db-backed
     * patterns and is newer than its database row.
     */
    public void sync(@NonNull String path) {
        syncDirectory(path);
    }

    /**
     * Like {@link #sync}, but no-ops when the same path was synced within {@link #SYNC_THROTTLE_MS}. For read paths
     * that run on every request (e.g. coverage reader acquisition); mutating operations call {@link #sync} directly and
     * always propagate immediately.
     */
    public void syncThrottled(@NonNull String path) {
        syncDirectoryThrottled(path);
    }

    /**
     * Removes the database rows of db-backed files no longer present under the given local directory, after a mutating
     * operation deleted them with raw file I/O (e.g. coverage removal, store deletion). The rows would otherwise
     * re-materialize the deleted files on every pod, and files later re-created at the same paths would inherit stale
     * content. Only call right after a local mutation, when the local directory is the authority on which files
     * survived it.
     */
    public void syncDeletions(@NonNull String path) {
        removeDbRowsOfDeletedFiles(path);
    }

    /**
     * Makes sure the directory exists in the local cache with all its database-backed files, for consumers that resolve
     * paths with raw file I/O against the resource loader's base directory instead of going through the Resource API.
     * No-ops if the same path was already materialized within {@link #MATERIALIZE_THROTTLE_MS} and its local db-backed
     * files are still present. Concurrent callers for the same path are single-flighted: one thread pulls, the others
     * wait for its result.
     */
    public void materialize(@NonNull String path) {
        materializeThrottled(path);
    }

    private void syncDirectoryThrottled(String path) {
        long now = System.currentTimeMillis();
        Long last = lastSynced.get(path);
        if (last != null && (now - last) < SYNC_THROTTLE_MS) {
            return;
        }
        syncDirectory(path);
    }

    private void syncDirectory(String path) {
        Path localDir = cache.localPath(path);
        if (!Files.isDirectory(localDir)) {
            return;
        }
        File[] files = localDir.toFile().listFiles(File::isFile);
        if (files == null) {
            return;
        }
        long newestSynced = newestSyncedMtime.getOrDefault(path, Long.MIN_VALUE);
        long maxSeen = newestSynced;
        for (File file : files) {
            String resourcePath = Paths.path(path, file.getName());
            if (!store.dbBackedFilePatterns().test(resourcePath)) {
                continue;
            }
            long localMtime = file.lastModified();
            maxSeen = Math.max(maxSeen, localMtime);
            if (localMtime > newestSynced) {
                push(resourcePath, file, localMtime);
            }
        }
        newestSyncedMtime.put(path, maxSeen);
        lastSynced.put(path, System.currentTimeMillis());
    }

    private void push(String resourcePath, File file, long localMtime) {
        Resource resource = store.get(resourcePath);
        boolean missing = resource.getType() == Type.UNDEFINED;
        if (!missing && localMtime <= resource.lastmodified()) {
            return;
        }
        log.debug("Persisting db-backed file {} to the resource store", resourcePath);
        try (InputStream in = new FileInputStream(file);
                OutputStream out = resource.out()) {
            in.transferTo(out);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to persist %s".formatted(resourcePath), e);
        }
    }

    private void removeDbRowsOfDeletedFiles(String path) {
        for (String resourcePath : store.dbBackedFilePaths(path)) {
            if (!Files.exists(cache.localPath(resourcePath))) {
                log.debug("Removing resource store row for locally deleted db-backed file {}", resourcePath);
                store.get(resourcePath).delete();
            }
        }
        // reset the path's bookkeeping: files later re-created at this path start from a clean slate
        // instead of inheriting the deleted files' newest-synced mtime or throttle stamps
        newestSyncedMtime.remove(path);
        lastMaterialized.remove(path);
        lastSynced.remove(path);
    }

    private void materializeThrottled(String path) {
        long now = System.currentTimeMillis();
        Long last = lastMaterialized.get(path);
        if (last != null && (now - last) < MATERIALIZE_THROTTLE_MS && hasLocalDbBackedFiles(path)) {
            return;
        }
        materializeSingleFlight(path);
    }

    private void materializeSingleFlight(String path) {
        CompletableFuture<Void> pull = new CompletableFuture<>();
        CompletableFuture<Void> inFlight = inFlightPulls.putIfAbsent(path, pull);
        if (inFlight != null) {
            // another thread is pulling this path; wait for its result instead of racing the same copies
            inFlight.join();
            return;
        }
        try {
            materializeDirectory(path);
            lastMaterialized.put(path, System.currentTimeMillis());
            pull.complete(null);
        } catch (RuntimeException e) {
            pull.completeExceptionally(e);
            throw e;
        } finally {
            inFlightPulls.remove(path);
            // no-op when a completion above already ran; releases waiters if an Error skipped both
            pull.completeExceptionally(
                    new IllegalStateException("Materialization of %s did not complete".formatted(path)));
        }
    }

    /**
     * Whether the directory holds at least one db-backed file in the local cache. The materialize throttle only holds
     * when it does: a directory that lost its local copies (cache eviction, or a read racing a write on another pod)
     * must re-pull from the database regardless of how recently it was materialized, or the consumer reads an empty
     * directory for the length of the throttle window.
     */
    private boolean hasLocalDbBackedFiles(String path) {
        File[] files = cache.localPath(path).toFile().listFiles(File::isFile);
        if (files == null) {
            return false;
        }
        for (File file : files) {
            if (store.dbBackedFilePatterns().test(Paths.path(path, file.getName()))) {
                return true;
            }
        }
        return false;
    }

    private void materializeDirectory(String path) {
        Resource resource = store.get(path);
        if (resource.getType() == Type.RESOURCE) {
            resource.file();
        } else {
            resource.dir();
        }
    }
}
