/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Weigher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.backend.pgconfig.cache.ClearableCache;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.jspecify.annotations.Nullable;

/**
 * The rows of the resource store without their content, loaded in one query, and the content read so far.
 *
 * <p>The index holds the whole store: a path missing from the index is missing from the store, and a lookup of it runs
 * no query. The index is loaded on first use, and again on the first use after {@link #clear()}. Threads wait for the
 * load. The store applies its own writes once they commit. The writes made while a load runs are applied to the loaded
 * rows too. Changes made outside the store, by another program or by an instance without the events, are not supported:
 * they are seen when the index is loaded again, after a reconnection of the event bus or a reset of GeoServer. A load
 * compares its rows with the rows of the load before it: the content of the changed rows is evicted, and the listener
 * set with {@link #onChangesFound(Consumer)} receives the changes.
 *
 * <p>The content is kept by row id, within a maximum weight in bytes. It stays until a write changes it, or the weight
 * bound removes it.
 *
 * <p>GeoServer empties the cache on a reset or a reload of its configuration, and
 * {@link org.geoserver.cloud.backend.pgconfig.cache.EntityCacheBusListener EntityCacheBusListener} empties it when the
 * event bus reconnects.
 */
@Slf4j(topic = "org.geoserver.cloud.backend.pgconfig.resource")
public class ResourceStoreCache implements GeoServerLifecycleHandler, ClearableCache {

    // A rough size of the key and of the entry of Caffeine for a content value
    private static final int CONTENT_OVERHEAD = 32;

    private final String name;
    private final Supplier<List<ResourceRow>> loader;
    private final Cache<Long, byte[]> content;

    private final ReentrantLock loadLock = new ReentrantLock();

    /** Orders the writes with the installation of a loaded index. */
    private final Object writeLock = new Object();

    /** The loaded rows; null before the first load and after {@link #clear()}. */
    private volatile @Nullable Index index;

    /** The writes made since the running load started; null when no load runs. Guarded by {@link #writeLock}. */
    private @Nullable List<Consumer<ConcurrentNavigableMap<String, ResourceRow>>> writesDuringLoad;

    /** Incremented by {@link #clear()}: a load started before it is not installed. Guarded by {@link #writeLock}. */
    private long generation;

    /**
     * The rows of the index dropped by {@link #clear()}, compared with the next load; null otherwise. Guarded by
     * {@link #writeLock}.
     */
    private @Nullable ConcurrentNavigableMap<String, ResourceRow> rowsBeforeClear;

    /** Receives the rows found changed by a load, see {@link #onChangesFound(Consumer)}. */
    private volatile Consumer<IndexChanges> changesListener = changes -> {};

    private record Index(ConcurrentNavigableMap<String, ResourceRow> rows) {}

    /** The index answering a read, and the changes found if the read loaded it. */
    private record Load(Index index, IndexChanges changes) {}

    /**
     * @param name the name of the cache in the metrics, such as {@code gs-resources}
     * @param loader the query of the rows of the store, without their content
     * @param contentMaximumWeightBytes the bound of the size of the content kept
     */
    public ResourceStoreCache(
            @NonNull String name, @NonNull Supplier<List<ResourceRow>> loader, long contentMaximumWeightBytes) {
        this.name = name;
        this.loader = loader;
        this.content = newContentCache(contentMaximumWeightBytes);
    }

    private static Cache<Long, byte[]> newContentCache(long maximumWeightBytes) {
        Weigher<Long, byte[]> weigher = (id, value) -> value.length + CONTENT_OVERHEAD;
        return Caffeine.newBuilder()
                .maximumWeight(maximumWeightBytes)
                .weigher(weigher)
                .recordStats()
                .build();
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * Sets the listener of the rows found changed by a load of the index. The listener runs on the thread of the load,
     * once the load is installed and its lock released. The first load finds no changes: there are no rows to compare
     * with.
     */
    void onChangesFound(@NonNull Consumer<IndexChanges> listener) {
        this.changesListener = listener;
    }

    /** Returns the row at the path, loading the index on first use and after a clear. */
    public Optional<ResourceRow> find(@NonNull String path) {
        return Optional.ofNullable(rows().get(path));
    }

    /** Returns the direct children of a directory, the rows of its subtree with the directory as parent. */
    public List<ResourceRow> children(@NonNull ResourceRow directory) {
        Collection<ResourceRow> descendants = subtree(rows(), directory.path()).values();
        return descendants.stream()
                .filter(row -> row.parentId() == directory.id())
                .toList();
    }

    /**
     * Returns the rows of the subtree of a path, without the row of the path. The root has the other rows as subtree.
     */
    public List<ResourceRow> descendants(@NonNull String path) {
        return List.copyOf(subtree(rows(), path).values());
    }

    /**
     * Returns the content of a row, read once by the reader and kept by id. The read runs inside the compute of the
     * content cache: concurrent reads of an id wait for one read, and a stalled read also blocks the other ids of its
     * bin in the map.
     */
    public byte[] content(long id, @NonNull Supplier<byte[]> reader) {
        return content.get(id, key -> reader.get());
    }

    /**
     * Adds or replaces the row of a path. A row with an older mtime than the indexed one does not replace it: two
     * commits of writes to one directory may apply their rows in either order. The database sets the mtime at the start
     * of the transaction: of two overlapping writes, the one started later keeps the entry until the next load.
     */
    public void put(@NonNull ResourceRow row) {
        write(rows -> rows.merge(row.path(), row, ResourceStoreCache::newer));
    }

    private static ResourceRow newer(ResourceRow indexed, ResourceRow put) {
        return put.mtime() >= indexed.mtime() ? put : indexed;
    }

    /** Removes the row of a path and the rows of its subtree, and evicts their content. */
    public void remove(@NonNull String path) {
        write(rows -> {
            List<ResourceRow> removed = new ArrayList<>(subtree(rows, path).values());
            Optional.ofNullable(rows.get(path)).ifPresent(removed::add);
            for (ResourceRow row : removed) {
                rows.remove(row.path());
                evictContent(row.id());
            }
        });
    }

    /**
     * Applies the move of a path and its subtree under another path. The moved rows are those read at the target path
     * and in its subtree once moved. The rows keep their id, and the content kept for them stays valid.
     *
     * <p>The entry at the previous path of a moved row is removed only if it has the id of that row. A load may already
     * hold the move, and a row created at the source path afterwards. A replay of the move on that load keeps the new
     * row.
     */
    public void moved(@NonNull String fromPath, @NonNull String toPath, @NonNull List<ResourceRow> movedRows) {
        write(rows -> {
            for (ResourceRow row : movedRows) {
                String previousPath = fromPath + row.path().substring(toPath.length());
                rows.computeIfPresent(previousPath, (path, indexed) -> indexed.id() == row.id() ? null : indexed);
                rows.merge(row.path(), row, ResourceStoreCache::newer);
            }
        });
    }

    /** Evicts the content kept for a row, once a write of its content commits. */
    public void evictContent(long id) {
        content.invalidate(id);
    }

    /**
     * Drops the index, loaded again on next use, and the content. The next load compares its rows with the dropped
     * ones.
     */
    @Override
    public void clear() {
        synchronized (writeLock) {
            generation++;
            Index dropped = index;
            if (dropped != null) {
                rowsBeforeClear = dropped.rows();
            }
            index = null;
        }
        content.invalidateAll();
    }

    /** Returns the number of rows in the index, 0 when it is not loaded. */
    public int size() {
        Index current = index;
        return current == null ? 0 : current.rows().size();
    }

    Cache<Long, byte[]> contentCache() {
        return content;
    }

    @Override
    public void onReset() {
        clear();
        log.info("onReset: cleared the cache {}", name);
    }

    @Override
    public void onDispose() {
        // nothing to release
    }

    @Override
    public void beforeReload() {
        // the reset following it at the start of the reload clears the cache
    }

    @Override
    public void onReload() {
        clear();
        log.info("onReload: cleared the cache {}", name);
    }

    /**
     * Returns the rows of the subtree of a path, as a view of the index. The keys starting with {@code path + "/"}
     * follow each other in the sorted map. The subtree of the root is the map without the root row.
     */
    private static ConcurrentNavigableMap<String, ResourceRow> subtree(
            ConcurrentNavigableMap<String, ResourceRow> rows, String path) {
        if (path.isEmpty()) {
            return rows.tailMap(path, false);
        }
        String prefix = path + "/";
        // '0' follows '/' in the character table: the range holds the keys starting with the prefix
        String end = path + "0";
        return rows.subMap(prefix, true, end, false);
    }

    private ConcurrentNavigableMap<String, ResourceRow> rows() {
        Index current = index;
        if (current == null) {
            current = loadWaitingForOthers();
        }
        return current.rows();
    }

    // Without an index, there are no rows to answer from
    private Index loadWaitingForOthers() {
        Load load;
        loadLock.lock();
        try {
            load = loadUnlessFresh();
        } finally {
            loadLock.unlock();
        }
        reportChanges(load.changes());
        return load.index();
    }

    // Another thread may have loaded the index while this one waited for the lock
    private Load loadUnlessFresh() {
        Index current = index;
        if (current != null) {
            return new Load(current, IndexChanges.NONE);
        }
        return load();
    }

    /**
     * Loads the rows, installs them, and evicts the content of the rows changed since the previous load. The caller
     * tells the listener of the changes once it released the load lock: a listener may wait for a lock held by a thread
     * waiting for the load, such as the lock of the notification dispatcher.
     */
    private Load load() {
        long loadGeneration = startRecordingWrites();
        List<ResourceRow> loaded;
        try {
            loaded = loader.get();
        } catch (RuntimeException e) {
            stopRecordingWrites();
            throw e;
        }
        ConcurrentNavigableMap<String, ResourceRow> rows = new ConcurrentSkipListMap<>();
        for (ResourceRow row : loaded) {
            rows.put(row.path(), row);
        }
        log.debug("Loaded the {} rows of the resource store", rows.size());
        Index loadedIndex = new Index(rows);
        Optional<ConcurrentNavigableMap<String, ResourceRow>> replaced = install(loadedIndex, loadGeneration);
        IndexChanges changes =
                replaced.map(previous -> evictChanged(previous, rows)).orElse(IndexChanges.NONE);
        return new Load(loadedIndex, changes);
    }

    /** Compares two loads, and evicts the content of the changed rows. */
    private IndexChanges evictChanged(
            ConcurrentNavigableMap<String, ResourceRow> previous, ConcurrentNavigableMap<String, ResourceRow> loaded) {
        IndexChanges changes = IndexChanges.between(previous, loaded);
        evictChangedContent(previous, changes);
        return changes;
    }

    /** Tells the listener of the changes found by a load. A failure of the listener is logged. */
    private void reportChanges(IndexChanges changes) {
        if (changes.isEmpty()) {
            return;
        }
        log.debug("A load of {} found changes made outside the store: {}", name, changes);
        try {
            changesListener.accept(changes);
        } catch (RuntimeException e) {
            log.warn("The listener of the changes found by a load of {} failed", name, e);
        }
    }

    // A modified path may hold a row with another id: the content of both ids is evicted
    private void evictChangedContent(ConcurrentNavigableMap<String, ResourceRow> previous, IndexChanges changes) {
        for (ResourceRow row : changes.modified()) {
            evictContent(row.id());
            Optional.ofNullable(previous.get(row.path())).ifPresent(replaced -> evictContent(replaced.id()));
        }
        for (ResourceRow row : changes.deleted()) {
            evictContent(row.id());
        }
    }

    private long startRecordingWrites() {
        synchronized (writeLock) {
            writesDuringLoad = new ArrayList<>();
            return generation;
        }
    }

    private void stopRecordingWrites() {
        synchronized (writeLock) {
            writesDuringLoad = null;
        }
    }

    /**
     * Applies the writes made during the load to the loaded rows, and makes them the index. A load started before a
     * {@link #clear()} answers the read that started it, and is not kept.
     *
     * <p>Returns the rows replaced by the load: those of the index, or those dropped by the last {@link #clear()}. The
     * result is empty for the first load, and for a load not kept.
     */
    private Optional<ConcurrentNavigableMap<String, ResourceRow>> install(Index loaded, long loadGeneration) {
        synchronized (writeLock) {
            List<Consumer<ConcurrentNavigableMap<String, ResourceRow>>> writes = writesDuringLoad;
            if (writes != null) {
                writes.forEach(change -> change.accept(loaded.rows()));
            }
            writesDuringLoad = null;
            if (generation != loadGeneration) {
                return Optional.empty();
            }
            Index previous = index;
            ConcurrentNavigableMap<String, ResourceRow> replaced = previous == null ? rowsBeforeClear : previous.rows();
            index = loaded;
            rowsBeforeClear = null;
            return Optional.ofNullable(replaced);
        }
    }

    /** Applies a write to the index, and records it for the load running, if any. */
    private void write(Consumer<ConcurrentNavigableMap<String, ResourceRow>> change) {
        synchronized (writeLock) {
            Index current = index;
            if (current != null) {
                change.accept(current.rows());
            }
            if (writesDuringLoad != null) {
                writesDuringLoad.add(change);
            }
        }
    }
}
