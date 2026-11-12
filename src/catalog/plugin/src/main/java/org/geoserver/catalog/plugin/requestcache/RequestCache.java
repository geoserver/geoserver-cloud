/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.requestcache;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * Remembers the catalog and configuration lookups of one request: the objects found, and the lookups finding nothing.
 * GeoServer looks the same objects up many times within a request, and each lookup in the facade below would decode the
 * object again.
 *
 * <p>The cache holds at most {@code maxEntries} entries. Past the cap, it takes no new entries and evicts none: the
 * objects repeated within a request, such as workspaces, stores and styles, are among the first looked up, and they
 * stay. The cap is checked without a lock. Puts from several threads at the same time may each pass the check, and each
 * may then add one entry over the cap.
 *
 * <p>The live caches of the JVM also share a {@link RequestCacheBudget}. A new entry takes a share of the budget, and a
 * dropped entry gives it back. Once the budget is used up, the cache rejects new entries as past its cap.
 *
 * <p>{@link RequestCacheScope} binds a cache to the threads of a request and kills it when the request ends. A dead
 * cache answers "not cached" to reads and ignores puts: a thread still holding it, such as a worker running after the
 * end of its request, reads through to the facade below.
 *
 * <p>A cache may also have a maximum age, and it dies by itself once that age is reached. The scope gives one to the
 * caches of work outside a request, see {@link RequestCacheScope#openIfAbsent(int)}.
 *
 * <p>A change made through another service instance is seen by the next lookup of the request: the lookup finds the
 * {@link RemoteChangeCounter} moved, and empties the cache once. The references already resolved inside the objects
 * held by the request keep their copy.
 */
public final class RequestCache {

    /** The value answered by {@link #found(Object)} for a lookup cached as finding nothing. */
    public static final Object MISS = new Object() {
        @Override
        public String toString() {
            return "RequestCache.MISS";
        }
    };

    /** The value of {@code maxAgeNanos} for a cache without a maximum age. */
    private static final long NO_MAXIMUM_AGE = Long.MAX_VALUE;

    private final Map<Object, Object> entries = new ConcurrentHashMap<>();
    private final int maxEntries;
    private final AtomicBoolean killed = new AtomicBoolean();

    /** The value of {@link System#nanoTime()} when the cache was created. */
    private final long createdAt = System.nanoTime();

    private final long maxAgeNanos;

    private final LongAdder hits = new LongAdder();
    private final LongAdder misses = new LongAdder();
    private final LongAdder rejected = new LongAdder();

    private final RequestCacheBudget budget;

    private final RemoteChangeCounter remoteChanges;

    /** The count of {@link #remoteChanges} when the cache opened, or when a lookup last found it moved. */
    private volatile long seenRemoteChanges;

    /** A cache of at most {@code maxEntries} entries, alive until it is killed, in the budget of the JVM. */
    public RequestCache(int maxEntries) {
        this(maxEntries, NO_MAXIMUM_AGE, RequestCacheBudget.shared(), RemoteChangeCounter.shared());
    }

    /** A cache of at most {@code maxEntries} entries, alive until it is killed, in the given budget. */
    RequestCache(int maxEntries, RequestCacheBudget budget) {
        this(maxEntries, NO_MAXIMUM_AGE, budget, RemoteChangeCounter.shared());
    }

    /** A cache in the given budget, following the given counter of the changes made through other instances. */
    RequestCache(int maxEntries, RequestCacheBudget budget, RemoteChangeCounter remoteChanges) {
        this(maxEntries, NO_MAXIMUM_AGE, budget, remoteChanges);
    }

    /**
     * A cache of at most {@code maxEntries} entries, in the budget of the JVM, dead once it is killed or {@code maxAge}
     * after its creation.
     */
    RequestCache(int maxEntries, Duration maxAge) {
        this(maxEntries, maxAge.toNanos(), RequestCacheBudget.shared(), RemoteChangeCounter.shared());
    }

    /** A cache dead once it is killed or {@code maxAge} after its creation, in the given budget. */
    RequestCache(int maxEntries, Duration maxAge, RequestCacheBudget budget) {
        this(maxEntries, maxAge.toNanos(), budget, RemoteChangeCounter.shared());
    }

    private RequestCache(
            int maxEntries, long maxAgeNanos, RequestCacheBudget budget, RemoteChangeCounter remoteChanges) {
        this.maxEntries = maxEntries;
        this.maxAgeNanos = maxAgeNanos;
        this.budget = budget;
        this.remoteChanges = remoteChanges;
        this.seenRemoteChanges = remoteChanges.count();
    }

    /**
     * Returns the object cached under the key, or null if the key is not cached or holds a miss. {@link #found(Object)}
     * tells the two apart.
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable T get(Object key) {
        Object value = found(key).orElse(null);
        if (value == MISS) {
            return null;
        }
        return (T) value;
    }

    /**
     * Returns what the cache holds under the key: empty if the key is not cached, {@link #MISS} if the lookup found
     * nothing, the object found otherwise. A read answered with an object or with {@link #MISS} counts as a hit, other
     * reads as misses.
     */
    public Optional<Object> found(Object key) {
        Object value = held(key);
        if (value == null) {
            misses.increment();
            return Optional.empty();
        }
        hits.increment();
        return Optional.of(value);
    }

    /**
     * Returns what the cache holds under the key, as {@link #found(Object)} does, but null if the key is not cached,
     * and without counting a read. A facade checks with it whether the request holds an object already: the check is
     * not a lookup of the request, and must not count as one.
     */
    @Nullable
    Object held(Object key) {
        if (isDead()) {
            return null;
        }
        followRemoteChanges();
        return entries.get(key);
    }

    /**
     * Empties the cache if the count of the changes made through other instances moved since the last check, and
     * records the new count. Returns whether the count moved.
     */
    private boolean followRemoteChanges() {
        long count = remoteChanges.count();
        if (count == seenRemoteChanges) {
            return false;
        }
        seenRemoteChanges = count;
        dropAll();
        return true;
    }

    /**
     * Caches the result of a lookup under the key: the object found, or a miss if the value is null. The put is
     * ignored, and counted as rejected, if the cache takes no entries, see {@link #takesEntries()}.
     *
     * <p>The put is also ignored, without being counted, if another instance changed the configuration since the last
     * lookup: the value may have been read before the change.
     */
    public void put(Object key, @Nullable Object value) {
        if (followRemoteChanges()) {
            return;
        }
        Object entry = value == null ? MISS : value;
        boolean stored = takesEntries() && store(key, entry);
        if (!stored) {
            rejected.increment();
        }
    }

    /**
     * Stores the entry. A new key takes a share of the budget first. Returns false if the budget is used up. A key
     * cached already keeps its share.
     */
    private boolean store(Object key, Object entry) {
        if (entries.replace(key, entry) != null) {
            return true;
        }
        if (!budget.tryTake()) {
            return false;
        }
        Object previous = entries.put(key, entry);
        if (previous != null) {
            // another thread stored the key after the replace, with a share of its own
            budget.release();
        }
        if (isDead()) {
            // kill() or the maximum age may have dropped the entries before this entry was stored
            drop(key);
        }
        return true;
    }

    /**
     * Whether the cache takes new entries: it is not dead, holds fewer than {@code maxEntries} entries, and the budget
     * is not used up. A full cache still answers from its entries until it dies.
     */
    public boolean takesEntries() {
        return !isDead() && !isFull() && !budget.isExhausted();
    }

    private boolean isFull() {
        return entries.size() >= maxEntries;
    }

    /** Removes the keys. Writes evict the keys of the changed objects: the request then sees its own writes. */
    public void evict(Object... keys) {
        for (Object key : keys) {
            drop(key);
        }
    }

    /**
     * Removes the keys matched by the predicate. A write uses it when the keys of the changed object cannot be listed
     * one by one, such as the keys of the lookups of a service by class.
     */
    public void evictIf(Predicate<Object> keyMatches) {
        for (Object key : entries.keySet()) {
            if (keyMatches.test(key)) {
                drop(key);
            }
        }
    }

    /**
     * Removes the entries. A change of a catalog object uses it: the cached objects may hold the changed object. Unlike
     * {@link #kill()}, the cache keeps taking entries.
     */
    public void clear() {
        dropAll();
    }

    /**
     * Ends the cache: later reads answer "not cached" and later puts are ignored. The entries are dropped at once,
     * because a task started by the request, such as a seed task, may hold the dead cache long after the request ended.
     *
     * <p>Returns true the first time, and false if the cache was killed already: the scope reports a cache to its
     * listeners from the first kill only.
     */
    public boolean kill() {
        boolean firstKill = killed.compareAndSet(false, true);
        if (firstKill) {
            dropAll();
        }
        return firstKill;
    }

    /**
     * Removes the entries one key at a time: each removal gives back its share of the budget once, even while another
     * thread puts or drops entries.
     */
    private void dropAll() {
        for (Object key : entries.keySet()) {
            drop(key);
        }
    }

    /** Removes the entry of the key, and gives back its share of the budget if the entry was there. */
    private void drop(Object key) {
        if (entries.remove(key) != null) {
            budget.release();
        }
    }

    /**
     * Whether the cache was killed, or reached its maximum age. A cache found at its maximum age drops its entries and
     * gives back their shares of the budget: a scope left open never runs its closer.
     */
    public boolean isDead() {
        if (killed.get()) {
            return true;
        }
        // the cache of a request has no maximum age, and its lookups never read the clock
        boolean expired = hasMaximumAge() && hasExpired(System.nanoTime());
        if (expired) {
            dropAll();
        }
        return expired;
    }

    /** Whether the cache dies by itself at its maximum age, as the cache of work outside a request does. */
    boolean hasMaximumAge() {
        return maxAgeNanos != NO_MAXIMUM_AGE;
    }

    /** Whether the cache reached its maximum age at {@code nanoTime}, a value of {@link System#nanoTime()}. */
    boolean hasExpired(long nanoTime) {
        long age = nanoTime - createdAt;
        return hasMaximumAge() && age >= maxAgeNanos;
    }

    /** The number of reads answered by the cache, with an object or with {@link #MISS}. */
    public long hits() {
        return hits.sum();
    }

    /** The number of reads not answered by the cache, the reads of a dead cache included. */
    public long misses() {
        return misses.sum();
    }

    /** The number of puts ignored because the cache was full or dead, or the budget was used up. */
    public long rejected() {
        return rejected.sum();
    }
}
