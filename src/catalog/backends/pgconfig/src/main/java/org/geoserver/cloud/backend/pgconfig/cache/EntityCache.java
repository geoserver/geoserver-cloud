/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Policy;
import com.github.benmanes.caffeine.cache.Ticker;
import com.github.benmanes.caffeine.cache.Weigher;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import lombok.NonNull;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey.Kind;

/**
 * Keeps the JSON of catalog and configuration objects in memory, shared by the requests of a service instance.
 *
 * <p>An {@code ENTITY} key holds the JSON of an object, and a {@code TILE_LAYER} key the GWC settings of a layer or
 * group, both encoded by an {@link EntityCodec}. {@code NAME} and {@code LOOKUP} keys hold the id of an object, in
 * UTF-8. A key can also hold a miss: the last query for the key found nothing. An empty value under a
 * {@code TILE_LAYER} key is not a miss: it says that the object has no tile layer.
 *
 * <p>A miss expires {@code missTtl} after it is stored. Other entries never expire: they stay until an eviction, a
 * {@link #clear()} or the bound of the size removes them. The size of the entries is bounded in bytes. An entry weighs
 * the length of its stored bytes plus an estimate for its key.
 *
 * <p>A put takes the {@link #epoch()} read by the caller before its query, and drops the value if an eviction happened
 * since. A writer evicts after its commit. Otherwise, a query started after the eviction could still read the old row,
 * and its put would pass the check. An eviction moves the epoch before and after its removal: a lookup made during the
 * removal may hit an entry being removed, and the epoch read by that lookup is out of date once the removal ends.
 *
 * <p>The cache also remembers the objects marked as uncacheable, as their rows could not be split into cache entries.
 * The repositories sharing the cache read such an object from the database. An eviction of its key forgets the mark,
 * and the next read tries the row again.
 *
 * <p>{@link EntityCacheMetrics} registers the statistics of the cache with Micrometer.
 */
public class EntityCache implements ClearableCache {

    // An estimate of the memory used by a key and its entry in the cache, beyond the stored bytes
    private static final int KEY_WEIGHT = 64;

    // Compared with ==: no array stored by a caller is this instance
    private static final byte[] MISS_SENTINEL = new byte[0];

    // Rows hold unexpected data only by accident: a small bound keeps a flood of them from using memory
    private static final int MAX_UNCACHEABLE = 1_000;

    // The longest expiry Caffeine takes: about 292 years
    private static final Duration NEVER = Duration.ofNanos(Long.MAX_VALUE);

    private final String name;
    private final long maximumWeight;
    private final EntityCodec codec;
    private final Cache<CacheKey, byte[]> entries;

    /**
     * The {@code ENTITY} keys of the objects marked as uncacheable: their rows could not be split into cache entries.
     */
    private final Cache<CacheKey, Boolean> uncacheable;

    private final AtomicLong epoch = new AtomicLong();
    private volatile boolean initialized;
    private volatile boolean suspended;

    /**
     * @param name the name of the cache in the metrics, such as {@code gs-catalog}
     * @param maximumWeightBytes the bound of the size of the entries, see {@link #maximumWeightFor(int)}
     * @param missTtl how long a miss stays after it is stored
     * @param codec encodes the JSON of the objects
     */
    public EntityCache(
            @NonNull String name, long maximumWeightBytes, @NonNull Duration missTtl, @NonNull EntityCodec codec) {
        this(name, maximumWeightBytes, missTtl, codec, Ticker.systemTicker());
    }

    EntityCache(String name, long maximumWeightBytes, Duration missTtl, EntityCodec codec, Ticker ticker) {
        this.name = name;
        this.maximumWeight = maximumWeightBytes;
        this.codec = codec;
        Expiry<CacheKey, byte[]> expiry = expireMissesAfterWrite(missTtl);
        this.entries = newCache(maximumWeightBytes, expiry, ticker);
        this.uncacheable = newUncacheableMarks();
    }

    /**
     * Returns a percentage of the maximum heap, in bytes, as the maximum weight of a cache.
     *
     * @throws IllegalArgumentException if the percentage is below 1 or above 100
     */
    public static long maximumWeightFor(int heapPercent) {
        requireHeapPercent(heapPercent);
        long maxMemory = Runtime.getRuntime().maxMemory();
        // Dividing first keeps the product within a long if the JVM reports no heap limit, as Long.MAX_VALUE
        return maxMemory / 100 * heapPercent;
    }

    /** Returns the name of the cache in the metrics, such as {@code gs-catalog}. */
    @Override
    public String name() {
        return name;
    }

    /** Returns the bound of the size of the entries, in bytes. */
    public long maximumWeight() {
        return maximumWeight;
    }

    /**
     * Returns a counter incremented by the evictions: {@link #evict}, {@link #evictWhere} and {@link #clear} increment
     * it before and after their removal. Read it before a database query, and pass it to the put of the result.
     */
    public long epoch() {
        return epoch.get();
    }

    /**
     * Returns what the cache holds for a key.
     *
     * <p>A {@link Lookup.Hit Hit} holds the JSON decoded by the codec for an {@code ENTITY} or {@code TILE_LAYER} key,
     * and the id of the object in UTF-8 for a {@code NAME} or {@code LOOKUP} key. A {@link Lookup.Miss Miss} means that
     * the last query for the key found nothing. {@link Lookup.Absent Absent} means that the key is not cached, or that
     * the cache is suspended.
     */
    public Lookup lookup(CacheKey key) {
        if (suspended) {
            return new Lookup.Absent();
        }
        byte[] stored = entries.getIfPresent(key);
        if (stored == null) {
            return new Lookup.Absent();
        }
        if (stored == MISS_SENTINEL) {
            return new Lookup.Miss();
        }
        byte[] value = decode(key, stored);
        return new Lookup.Hit(value);
    }

    /**
     * Returns what the cache holds for an object under several keys, one per possible type: a store looked up by id may
     * be a data store or a coverage store. Returns the first {@link Lookup.Hit Hit}. Otherwise returns
     * {@link Lookup.Absent Absent} if a key is absent, as a query must run, and {@link Lookup.Miss Miss} if each key
     * holds a miss. Absent for no keys.
     *
     * <p>The keys are probed without side effects first. The statistics then record one get, for the key giving the
     * answer, as for a lookup of one key. Another thread may write that key between the probe and the get. The get then
     * decides: a value gives a Hit. A miss gives a Miss only if the probe found a miss under each key. Anything else
     * gives Absent, and the caller runs its query.
     */
    public Lookup lookup(List<CacheKey> keys) {
        if (suspended || keys.isEmpty()) {
            return new Lookup.Absent();
        }
        Probe probe = probe(keys);
        Lookup found = lookup(probe.answeringKey());
        return switch (found) {
            case Lookup.Hit hit -> hit;
            case Lookup.Miss miss when probe.eachKeyMissed() -> miss;
            case Lookup.Miss _, Lookup.Absent _ -> new Lookup.Absent();
        };
    }

    /** The key giving the answer of a lookup under several keys, and whether each key held a miss at the probe. */
    private record Probe(CacheKey answeringKey, boolean eachKeyMissed) {}

    // The first key holding a value, else the first absent key, else the first key: each one holds a miss
    private Probe probe(List<CacheKey> keys) {
        Policy<CacheKey, byte[]> policy = entries.policy();
        CacheKey absent = null;
        for (CacheKey key : keys) {
            byte[] stored = policy.getIfPresentQuietly(key);
            if (null != stored && stored != MISS_SENTINEL) {
                return new Probe(key, false);
            }
            if (null == stored && null == absent) {
                absent = key;
            }
        }
        if (null == absent) {
            return new Probe(keys.getFirst(), true);
        }
        return new Probe(absent, false);
    }

    /**
     * Returns the id held by a {@code NAME} or {@code LOOKUP} key. Empty if the key holds a miss or is not cached:
     * {@link #lookup(CacheKey)} tells the two apart.
     *
     * @throws IllegalArgumentException for an {@code ENTITY} or {@code TILE_LAYER} key
     */
    public Optional<String> lookupId(CacheKey nameOrLookupKey) {
        requireIdKey(nameOrLookupKey);
        Lookup found = lookup(nameOrLookupKey);
        if (found instanceof Lookup.Hit hit) {
            String id = new String(hit.json(), UTF_8);
            return Optional.of(id);
        }
        return Optional.empty();
    }

    /**
     * Stores the JSON of an object under its {@code ENTITY} key, or the GWC settings of a layer or group under its
     * {@code TILE_LAYER} key. Ignored while the cache is suspended. The cache may keep the array, as {@link NoopCodec}
     * returns its argument: callers must not modify it after the put.
     *
     * @param epochSeen the {@link #epoch()} read before the query for the JSON. If an eviction happened since, the JSON
     *     may be stale: the put drops it, and evicts the key.
     * @return the weight of the stored entry, 0 if nothing was stored
     * @throws IllegalArgumentException for a {@code NAME} or {@code LOOKUP} key
     */
    public long put(CacheKey key, byte[] json, long epochSeen) {
        requireJsonKey(key);
        if (suspended) {
            return 0;
        }
        byte[] encoded = codec.encode(json);
        return store(key, encoded, epochSeen);
    }

    /**
     * Stores the JSON read by a listing, as {@link #put} does. Ignored until the cache is initialized.
     *
     * @throws IllegalArgumentException for a {@code NAME} or {@code LOOKUP} key, initialized or not
     */
    public void putFromListing(CacheKey key, byte[] json, long epochSeen) {
        requireJsonKey(key);
        if (initialized) {
            put(key, json, epochSeen);
        }
    }

    /**
     * Whether a lookup of the key would answer a {@link Lookup.Hit Hit}. False for a miss, and while the cache is
     * suspended. Unlike {@link #lookup(CacheKey)}, it decodes nothing, and leaves the statistics, the eviction order
     * and the expiry of the entry as they are.
     */
    public boolean contains(CacheKey key) {
        if (suspended) {
            return false;
        }
        byte[] stored = entries.policy().getIfPresentQuietly(key);
        return stored != null && stored != MISS_SENTINEL;
    }

    /**
     * Stores the id of an object read by a listing, as {@link #putId} does. Ignored until the cache is initialized.
     *
     * @throws IllegalArgumentException for an {@code ENTITY} or {@code TILE_LAYER} key, initialized or not
     */
    public void putIdFromListing(CacheKey key, String id, long epochSeen) {
        requireIdKey(key);
        if (initialized) {
            putId(key, id, epochSeen);
        }
    }

    /**
     * Stores the id of an object under a {@code NAME} or {@code LOOKUP} key. Ignored while the cache is suspended.
     *
     * @param epochSeen the {@link #epoch()} read before the query for the id, as for {@link #put}
     * @return the weight of the stored entry, 0 if nothing was stored
     * @throws IllegalArgumentException for an {@code ENTITY} or {@code TILE_LAYER} key
     */
    public long putId(CacheKey key, String id, long epochSeen) {
        requireIdKey(key);
        if (suspended) {
            return 0;
        }
        byte[] utf8 = id.getBytes(UTF_8);
        return store(key, utf8, epochSeen);
    }

    /**
     * Remembers that the query for a key found nothing. Ignored until the cache is initialized, and while it is
     * suspended.
     *
     * @param epochSeen the {@link #epoch()} read before the query, as for {@link #put}
     */
    public void putMiss(CacheKey key, long epochSeen) {
        if (initialized && !suspended) {
            store(key, MISS_SENTINEL, epochSeen);
        }
    }

    /**
     * Marks an object as uncacheable: its row could not be split into cache entries. Readers then read the object from
     * the database without splitting its row again, until an eviction of its key or a clear of the cache.
     *
     * @return true if the object was not marked yet
     * @throws IllegalArgumentException for a {@code NAME}, {@code LOOKUP} or {@code TILE_LAYER} key
     */
    public boolean markUncacheable(CacheKey key) {
        requireEntityKey(key);
        // Unlike a put, a mark does not check the epoch. A mark landing after the eviction of a concurrent write
        // stays until the next eviction of its key or clear: the object is read from the database meanwhile, never
        // stale.
        return null == uncacheable.asMap().putIfAbsent(key, Boolean.TRUE);
    }

    /** Whether an object is marked as uncacheable, see {@link #markUncacheable(CacheKey)}. */
    public boolean isUncacheable(CacheKey key) {
        return null != uncacheable.getIfPresent(key);
    }

    /**
     * Whether an object is marked as uncacheable. Finding the key of a row costs a parse of its JSON, and readers skip
     * it while no object is marked.
     */
    public boolean hasUncacheableRows() {
        return uncacheable.estimatedSize() > 0;
    }

    /** Evicts the keys, and increments the {@link #epoch()} before and after their removal. */
    public void evict(CacheKey... keys) {
        moveEpoch();
        List<CacheKey> evicted = List.of(keys);
        entries.invalidateAll(evicted);
        uncacheable.invalidateAll(evicted);
        moveEpoch();
    }

    /**
     * Evicts the keys accepted by a predicate, and increments the {@link #epoch()} before and after their removal.
     * Scans the keys without reading the values, for rare events such as the rename of a workspace.
     */
    public void evictWhere(Predicate<CacheKey> which) {
        moveEpoch();
        Set<CacheKey> keys = entries.asMap().keySet();
        keys.removeIf(which);
        Set<CacheKey> uncacheableKeys = uncacheable.asMap().keySet();
        uncacheableKeys.removeIf(which);
        moveEpoch();
    }

    /** Empties the cache, and increments the {@link #epoch()} before and after the removal. */
    @Override
    public void clear() {
        moveEpoch();
        entries.invalidateAll();
        uncacheable.invalidateAll();
        moveEpoch();
    }

    /**
     * Sets whether the catalog loader has initialized the catalog. Until then, the cache keeps no misses and no objects
     * read by listings. A miss kept during loading would make pods create the same default objects twice, for example
     * two pods starting on an empty database.
     */
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    /**
     * Suspends the cache, for example while the catalog reloads. A suspended cache answers {@link Lookup.Absent Absent}
     * to lookups and ignores puts. Evictions still apply. The entries stored before the suspension stay, and answer
     * lookups again once it ends.
     */
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    /** Whether the cache is suspended, see {@link #setSuspended(boolean)}. */
    public boolean isSuspended() {
        return suspended;
    }

    private static Expiry<CacheKey, byte[]> expireMissesAfterWrite(Duration missTtl) {
        return Expiry.writing((key, value) -> value == MISS_SENTINEL ? missTtl : NEVER);
    }

    private static Cache<CacheKey, byte[]> newCache(
            long maximumWeightBytes, Expiry<CacheKey, byte[]> expiry, Ticker ticker) {
        Weigher<CacheKey, byte[]> weigher = (key, value) -> value.length + KEY_WEIGHT;
        return Caffeine.newBuilder()
                .maximumWeight(maximumWeightBytes)
                .weigher(weigher)
                .expireAfter(expiry)
                .ticker(ticker)
                .recordStats()
                .build();
    }

    private static Cache<CacheKey, Boolean> newUncacheableMarks() {
        return Caffeine.newBuilder().maximumSize(MAX_UNCACHEABLE).build();
    }

    private static void requireHeapPercent(int heapPercent) {
        if (heapPercent < 1 || heapPercent > 100) {
            throw new IllegalArgumentException(
                    "geoserver.backend.pgconfig.cache.max-heap-percent must be between 1 and 100, got " + heapPercent);
        }
    }

    // Ids are short: compressing them would save little, and cost a decode on each lookup by name
    private byte[] decode(CacheKey key, byte[] stored) {
        if (key.holdsJson()) {
            return codec.decode(stored);
        }
        return stored;
    }

    // lookup reads the bytes of a key holding JSON through the codec, and those of the other keys as an id. Bytes
    // stored under a key of the wrong kind would read back as garbage.
    private static void requireIdKey(CacheKey key) {
        if (key.holdsJson()) {
            throw new IllegalArgumentException("Expected a NAME or LOOKUP key, got " + key);
        }
    }

    private static void requireJsonKey(CacheKey key) {
        if (!key.holdsJson()) {
            throw new IllegalArgumentException("Expected an ENTITY or TILE_LAYER key, got " + key);
        }
    }

    private static void requireEntityKey(CacheKey key) {
        if (key.kind() != Kind.ENTITY) {
            throw new IllegalArgumentException("Expected an ENTITY key, got " + key);
        }
    }

    // Drops the value if an eviction ran after the caller read the epoch, possibly during its query
    private long store(CacheKey key, byte[] value, long epochSeen) {
        ConcurrentMap<CacheKey, byte[]> map = entries.asMap();
        // evict() waits for the key lock held by the compute. Returning the current value would restart its expiry.
        map.compute(key, (k, current) -> epoch.get() == epochSeen ? value : null);
        // The scans of evictWhere() and clear() skip a key while its insertion is in progress, and miss the value
        if (epoch.get() != epochSeen) {
            map.remove(key, value);
            return 0;
        }
        return (long) value.length + KEY_WEIGHT;
    }

    /**
     * Increments the epoch, before and after a removal of keys. After the first move, a put of a value read before the
     * removal drops its value. A lookup during the removal may still hit an entry being removed. After the second move,
     * the epoch read by that lookup is out of date: a cache of objects built from entries, kept under the epoch read
     * before the build, then drops what was built from the removed entries.
     */
    private void moveEpoch() {
        epoch.incrementAndGet();
    }

    /**
     * Returns the Caffeine cache, for {@link EntityCacheMetrics}. Callers must not modify it: a put or a removal
     * through it would skip the codec, the epoch and the flags.
     */
    Cache<CacheKey, byte[]> caffeine() {
        return entries;
    }

    /** Runs the pending maintenance of the cache, such as evictions by size, in the calling thread. For tests. */
    void cleanUp() {
        entries.cleanUp();
    }
}
