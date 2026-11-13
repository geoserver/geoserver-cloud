/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.config;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.sql.DataSource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Info;
import org.geoserver.cloud.backend.pgconfig.cache.CacheAccess;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.ConfigInfoKeys;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * The {@link EntityCache} as the configuration repository uses it. The repository keeps the SQL, and passes its queries
 * as functions returning the JSON of the object found.
 *
 * <p>An object is decoded from its own JSON, in the cache or in its row. Its workspace is then a proxy holding the id
 * of the workspace, resolved by the facade as for an object read without the cache. Puts follow the epoch protocol of
 * the cache: the epoch is read before the query. {@link CacheAccess} decides when reads use the cache, and when the
 * evictions of writes run.
 *
 * <p>If the JSON of an object in the cache cannot be decoded, the object is read from the database instead. The first
 * failure logs a warning and marks the key as uncacheable. Later reads of the key skip the cache until a write evicts
 * the key or the cache is cleared.
 */
@Slf4j(topic = "org.geoserver.cloud.backend.pgconfig.config")
final class ConfigRepositoryCache {

    private final EntityCache cache;
    private final CacheAccess access;
    private final ObjectMapper infoMapper;

    ConfigRepositoryCache(
            @NonNull EntityCache cache, @NonNull DataSource dataSource, @NonNull ObjectMapper infoMapper) {
        this.cache = cache;
        this.access = new CacheAccess(cache, dataSource);
        this.infoMapper = infoMapper;
    }

    /** Whether reads go through the cache, see {@link CacheAccess#inUse()}. */
    boolean inUse() {
        return access.inUse();
    }

    /** Returns the epoch of the cache, read by a listing before its query. */
    long epoch() {
        return cache.epoch();
    }

    /**
     * Finds an object by its {@code ENTITY} key: decoded from its JSON in the cache, or found by the query. The query
     * feeds the cache, and an empty answer is remembered as a miss.
     */
    <T extends Info> Optional<T> find(CacheKey key, Class<T> type, Supplier<Optional<String>> query) {
        if (cache.isUncacheable(key)) {
            logReadWithoutCache(key);
            return readWithoutCache(type, query);
        }
        return switch (cache.lookup(key)) {
            case Lookup.Hit hit -> decodeCached(key, hit.json(), type).or(() -> readWithoutCache(type, query));
            case Lookup.Miss() -> Optional.empty();
            case Lookup.Absent() -> queryRemembering(key, type, query);
        };
    }

    /**
     * Finds an object through the {@code LOOKUP} key of a query, such as the key of the settings of a workspace. The
     * key holds the id of the object found by the query. The object comes from the cache if the cache holds it and the
     * object answers the lookup. Otherwise the query runs, and the object found is stored with its id under the key. If
     * the query finds nothing, the key keeps a miss when the lookup remembers misses.
     */
    <T extends Info> Optional<T> findThrough(IdLookup<T> lookup, Supplier<Optional<String>> query) {
        return switch (cache.lookup(lookup.key())) {
            case Lookup.Hit hit -> answerFromHint(lookup, hit, query);
            case Lookup.Miss() -> Optional.empty();
            case Lookup.Absent() -> queryThrough(lookup, query);
        };
    }

    /**
     * Decodes an object read by a listing, and puts its JSON in the cache. The cache ignores the put until it is
     * initialized.
     *
     * @param epochSeen the epoch read before the query of the listing
     */
    <T extends Info> T readListed(String text, Class<T> type, Function<String, CacheKey> entityKeys, long epochSeen) {
        byte[] json = text.getBytes(UTF_8);
        T object = decode(json, type);
        cache.putFromListing(entityKeys.apply(object.getId()), json, epochSeen);
        return object;
    }

    /** Evicts the keys made stale by a write, once the write commits, see {@link CacheAccess}. */
    void evictAfterWrite(Collection<CacheKey> keys) {
        access.evictAfterWrite(keys);
    }

    private <T extends Info> Optional<T> queryRemembering(
            CacheKey key, Class<T> type, Supplier<Optional<String>> query) {
        long epochSeen = cache.epoch();
        Optional<Row<T>> row = fetch(type, query);
        if (row.isPresent()) {
            cache.put(key, row.get().json(), epochSeen);
        } else {
            cache.putMiss(key, epochSeen);
        }
        return row.map(Row::object);
    }

    private <T extends Info> Optional<T> readWithoutCache(Class<T> type, Supplier<Optional<String>> query) {
        return fetch(type, query).map(Row::object);
    }

    /**
     * Answers a lookup with the object of the id held by its key, if the cache holds the object, and runs the query
     * otherwise.
     *
     * <p>The id is a hint. Another service instance may rename the object, or move it to another workspace, and a
     * listing of this instance may cache the changed object before the event of the change arrives. If the cached
     * object does not answer the lookup, the hint is stale: the key is evicted, and the query stores the fresh hint.
     */
    private <T extends Info> Optional<T> answerFromHint(
            IdLookup<T> lookup, Lookup.Hit idHit, Supplier<Optional<String>> query) {
        String id = new String(idHit.json(), UTF_8);
        Optional<T> cached = cachedObject(lookup.entityKey(id), lookup.type());
        if (cached.isEmpty()) {
            return queryThrough(lookup, query);
        }
        if (lookup.answeredBy(cached.get())) {
            return cached;
        }
        cache.evict(lookup.key());
        return queryThrough(lookup, query);
    }

    /** The object held by an {@code ENTITY} key. Empty if the cache does not hold it: no query runs. */
    private <T extends Info> Optional<T> cachedObject(CacheKey key, Class<T> type) {
        if (!cache.isUncacheable(key) && cache.lookup(key) instanceof Lookup.Hit hit) {
            return decodeCached(key, hit.json(), type);
        }
        return Optional.empty();
    }

    private <T extends Info> Optional<T> queryThrough(IdLookup<T> lookup, Supplier<Optional<String>> query) {
        long epochSeen = cache.epoch();
        Optional<Row<T>> row = fetch(lookup.type(), query);
        if (row.isPresent()) {
            putFound(lookup, row.get(), epochSeen);
        } else if (lookup.remembersMiss()) {
            cache.putMiss(lookup.key(), epochSeen);
        }
        return row.map(Row::object);
    }

    /**
     * Stores the object found by a lookup under its own key, and its id under the key of the lookup. An object marked
     * as uncacheable keeps its JSON out of the cache: the lookup reads it from the database.
     */
    private <T extends Info> void putFound(IdLookup<T> lookup, Row<T> row, long epochSeen) {
        String id = row.object().getId();
        CacheKey entityKey = lookup.entityKey(id);
        if (!cache.isUncacheable(entityKey)) {
            cache.put(entityKey, row.json(), epochSeen);
        }
        cache.putId(lookup.key(), id, epochSeen);
    }

    /** Runs a query, and decodes the object of the row found from its JSON. */
    private <T extends Info> Optional<Row<T>> fetch(Class<T> type, Supplier<Optional<String>> query) {
        return query.get().map(text -> {
            byte[] json = text.getBytes(UTF_8);
            return new Row<>(json, decode(json, type));
        });
    }

    private <T> T decode(byte[] json, Class<T> type) {
        return infoMapper.readValue(json, type);
    }

    /**
     * Decodes an object from its JSON in the cache. Empty if the JSON cannot be decoded: the key is then marked as
     * uncacheable, and the caller reads the object from the database.
     */
    private <T> Optional<T> decodeCached(CacheKey key, byte[] json, Class<T> type) {
        try {
            return Optional.of(decode(json, type));
        } catch (JacksonException e) {
            markUncacheable(key, e);
            return Optional.empty();
        }
    }

    /** Marks a key as uncacheable. The first failure of the key logs a warning, a later one a debug line. */
    private void markUncacheable(CacheKey key, JacksonException decodeError) {
        if (cache.markUncacheable(key)) {
            log.warn(
                    "{} {} is read from the database, its JSON in the cache could not be decoded: {}",
                    key.type(),
                    key.key(),
                    decodeError.toString());
        } else {
            logReadWithoutCache(key);
        }
    }

    private static void logReadWithoutCache(CacheKey key) {
        log.debug("{} {} is read from the database, its JSON in the cache could not be decoded", key.type(), key.key());
    }

    /** A row found by a query: the JSON of its object, and the object decoded from it. */
    private record Row<T>(byte[] json, T object) {}

    /**
     * A lookup of an object through a {@code LOOKUP} key holding its id.
     *
     * @param key the key holding the id of the object found, or a miss
     * @param type the type to decode the object as, such as {@code ServiceInfo}
     * @param entityKeys the {@code ENTITY} key of an id, such as {@link ConfigInfoKeys#service(String)}
     * @param answers whether an object answers the lookup: the class asked by the caller, and the name or the workspace
     *     held by the key. A cached object failing it makes the id held by the key stale.
     * @param remembersMiss whether a query finding nothing stores a miss under the key. A query narrower than its key
     *     must not: services of different classes may share a name, and the miss of one class under the key of the name
     *     would hide the others.
     */
    record IdLookup<T extends Info>(
            CacheKey key,
            Class<T> type,
            Function<String, CacheKey> entityKeys,
            Predicate<T> answers,
            boolean remembersMiss) {

        CacheKey entityKey(String id) {
            return entityKeys.apply(id);
        }

        boolean answeredBy(T object) {
            return answers.test(object);
        }
    }
}
