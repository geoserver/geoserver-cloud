/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.cloud.backend.pgconfig.cache.CacheAccess;
import org.geoserver.cloud.backend.pgconfig.cache.CacheEntry;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey.Kind;
import org.geoserver.cloud.backend.pgconfig.cache.CatalogInfoKeys;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup;
import org.geoserver.cloud.backend.pgconfig.cache.PutBudget;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.jspecify.annotations.Nullable;

/**
 * The {@link EntityCache} as a repository uses it: the cache side of the lookups, listings and writes of the
 * repository. The repository keeps the SQL, and passes its queries as functions.
 *
 * <p>An object has a key for each of its possible types: a store looked up by id may be a data store or a coverage
 * store. Puts follow the epoch protocol of the cache: the epoch is read before the query. {@link CacheAccess} decides
 * when lookups use the cache, and runs the evictions of writes after the statement, or after the transaction when one
 * runs on the data source of the repository.
 *
 * <p>A row that cannot be split into cache entries is read as without a cache. Its first failure logs a warning with
 * the type and id of the row, and marks the object as uncacheable in the {@link EntityCache}. Later reads of the row
 * skip the split until the row is written or the cache is cleared. The repositories sharing the cache share the marks:
 * a write through one repository lets the others try the row again.
 */
@Slf4j(topic = "org.geoserver.cloud.backend.pgconfig.catalog.repository")
final class RepositoryCache<T extends CatalogInfo> {

    private final EntityCache cache;
    private final CacheAccess access;
    private final Class<T> contentType;
    private final RowTypes rowTypes;

    RepositoryCache(
            @NonNull EntityCache cache,
            @NonNull DataSource dataSource,
            @NonNull Class<T> contentType,
            @NonNull RowTypes rowTypes) {
        this.cache = cache;
        this.access = new CacheAccess(cache, dataSource);
        this.contentType = contentType;
        this.rowTypes = rowTypes;
    }

    /** Whether lookups go through the cache, see {@link CacheAccess#inUse()}. */
    boolean inUse() {
        return access.inUse();
    }

    long epoch() {
        return cache.epoch();
    }

    /** Looks an object up under its keys, as {@link EntityCache#lookup(List)} does. */
    Lookup lookup(List<CacheKey> keys) {
        return cache.lookup(keys);
    }

    /** The {@code ENTITY} keys of an id, one per type of the class. */
    List<CacheKey> entityKeys(String id, Class<?> clazz) {
        return entityKeys(rowTypes, id, clazz);
    }

    private static List<CacheKey> entityKeys(RowTypes types, String id, Class<?> clazz) {
        return types.assignableTo(clazz).stream()
                .map(type -> CacheKey.entity(type, id))
                .toList();
    }

    /** The {@code NAME} keys of a prefixed name, one per type of the class. */
    List<CacheKey> nameKeys(String prefixedName, Class<?> clazz) {
        return rowTypes.assignableTo(clazz).stream()
                .map(type -> CacheKey.name(type, prefixedName))
                .toList();
    }

    /**
     * The keys of {@link CatalogInfoKeys#globalNameMiss(ConfigInfoType, String)} for a name, one per type of the class.
     */
    List<CacheKey> globalNameMissKeys(String name, Class<?> clazz) {
        return rowTypes.assignableTo(clazz).stream()
                .map(type -> CatalogInfoKeys.globalNameMiss(type, name))
                .toList();
    }

    /** Decodes an object from its own JSON in the cache. Its references are proxies holding ids. */
    T decode(byte[] json) {
        return CatalogInfoRowMapper.decodeObject(json, contentType);
    }

    /**
     * Returns an object held by the cache, decoded from its own JSON: its references are proxies holding ids. The
     * object may belong to another repository, as the resource of a layer does. Empty if the cache does not hold it: no
     * query runs.
     */
    <R extends CatalogInfo> Optional<R> cachedObject(String id, Class<R> clazz) {
        List<CacheKey> keys = entityKeys(new RowTypes(clazz), id, clazz);
        if (cache.lookup(keys) instanceof Lookup.Hit hit) {
            R object = CatalogInfoRowMapper.decodeObject(hit.json(), clazz);
            return Optional.of(object);
        }
        return Optional.empty();
    }

    /**
     * Runs the query of a lookup absent from the cache. The query puts its rows in the cache. If it finds the object,
     * the {@code LOOKUP} keys get its id. If not, the keys remember the miss.
     */
    <U extends T> Optional<U> queryRemembering(List<CacheKey> keys, Supplier<Optional<U>> query) {
        long epochSeen = cache.epoch();
        Optional<U> found = query.get();
        if (found.isPresent()) {
            putLookupIds(keys, found.get().getId(), epochSeen);
        } else {
            putMisses(keys, epochSeen);
        }
        return found;
    }

    /** Stores the id of an object found by a lookup under its {@code LOOKUP} keys. Its row holds its names. */
    private void putLookupIds(List<CacheKey> keys, String id, long epochSeen) {
        for (CacheKey key : keys) {
            if (key.kind() == Kind.LOOKUP) {
                cache.putId(key, id, epochSeen);
            }
        }
    }

    /** Remembers that the query for the keys found nothing. */
    private void putMisses(List<CacheKey> keys, long epochSeen) {
        for (CacheKey key : keys) {
            cache.putMiss(key, epochSeen);
        }
    }

    /** Remembers a miss for the ids missing from the fetched objects, under the keys of each type of the class. */
    void rememberMissing(List<String> ids, List<T> fetched, Class<?> type, long epochSeen) {
        Set<String> foundIds = fetched.stream().map(CatalogInfo::getId).collect(Collectors.toSet());
        for (String id : ids) {
            if (!foundIds.contains(id)) {
                putMisses(entityKeys(id, type), epochSeen);
            }
        }
    }

    /**
     * Puts the entries of a row read by a lookup in the cache, and returns the object of the row decoded from its own
     * JSON. A row that cannot be split into entries is mapped as without a cache, and left out of the cache.
     *
     * <p>If the thread has a {@link PutBudget}, the weight of the entries put is charged to it. Once it is spent, the
     * row is mapped as without a cache and not put. A layer comes with the references held by its row. The members and
     * styles of a group are looked up above the repository, and those rows are not put either. The row mapper loads a
     * style of another workspace through the style repository.
     */
    @Nullable
    T readRow(CatalogInfoRowMapper<T> rowMapper, ResultSet rs, int rowNum, long epochSeen) throws SQLException {
        Optional<PutBudget> budget = PutBudget.current();
        if (budget.isPresent() && budget.get().isSpent()) {
            return rowMapper.mapRow(rs, rowNum);
        }
        if (isUncacheableRow(rowMapper, rs)) {
            return mapUncacheableRow(rowMapper, rs, rowNum);
        }
        List<CacheEntry> entries;
        try {
            entries = rowMapper.decompose(rs);
        } catch (SQLException | UncheckedSqlException | IllegalArgumentException e) {
            T object = rowMapper.mapRow(rs, rowNum);
            markUncacheable(object, e);
            return object;
        }
        if (entries.isEmpty()) {
            return null;
        }
        long weight = putRow(entries, epochSeen);
        budget.ifPresent(b -> b.charge(weight));
        return decode(entries.getFirst().json());
    }

    // Reading the key of a row parses its own column: it runs only while the cache holds an uncacheable object
    private boolean isUncacheableRow(CatalogInfoRowMapper<T> rowMapper, ResultSet rs) {
        if (!cache.hasUncacheableRows()) {
            return false;
        }
        Optional<CacheKey> key = rowKey(rowMapper, rs);
        return key.isPresent() && cache.isUncacheable(key.get());
    }

    // A key that cannot be read leaves the row to the split: the split reports the failure
    private Optional<CacheKey> rowKey(CatalogInfoRowMapper<T> rowMapper, ResultSet rs) {
        try {
            return rowMapper.rowKey(rs);
        } catch (SQLException | UncheckedSqlException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private T mapUncacheableRow(CatalogInfoRowMapper<T> rowMapper, ResultSet rs, int rowNum) throws SQLException {
        T object = rowMapper.mapRow(rs, rowNum);
        if (null != object) {
            logReadWithoutCache(keyOf(object));
        }
        return object;
    }

    private static void logReadWithoutCache(CacheKey key) {
        log.debug("{} {} is read without the cache, its row could not be split before", key.type(), key.key());
    }

    /**
     * Marks the object of a row that could not be split as uncacheable. The first failure of the row logs a warning, a
     * later one a debug line.
     */
    private void markUncacheable(@Nullable T object, Exception splitError) {
        if (null == object) {
            return;
        }
        CacheKey key = keyOf(object);
        boolean first = cache.markUncacheable(key);
        if (first) {
            log.warn(
                    "{} {} is not cached, its row could not be split into cache entries: {}",
                    key.type(),
                    key.key(),
                    splitError.toString());
        } else {
            log.debug("{} {} is not cached, its row could not be split into cache entries", key.type(), key.key());
        }
    }

    /** Stores the entries of a row read by a lookup, and returns the weight stored. */
    private long putRow(List<CacheEntry> entries, long epochSeen) {
        long weight = 0;
        for (CacheEntry entry : entries) {
            if (entry.key().holdsJson()) {
                weight += cache.put(entry.key(), entry.json(), epochSeen);
            } else {
                weight += cache.putId(entry.key(), entry.id(), epochSeen);
            }
        }
        return weight;
    }

    /**
     * Puts the entries of a row read by a listing in the cache. The listing maps its rows as without a cache, and a row
     * that cannot be split into entries is left out of the cache.
     */
    void putListedRow(CatalogInfoRowMapper<T> rowMapper, ResultSet rs, @Nullable T object, long epochSeen) {
        if (null == object) {
            return;
        }
        CacheKey key = keyOf(object);
        if (cache.isUncacheable(key)) {
            logReadWithoutCache(key);
            return;
        }
        try {
            List<CacheEntry> entries = rowMapper.decompose(rs);
            putListedEntries(entries, epochSeen);
        } catch (SQLException | UncheckedSqlException | IllegalArgumentException e) {
            markUncacheable(object, e);
        }
    }

    private static CacheKey keyOf(CatalogInfo object) {
        return CacheKey.entity(ConfigInfoType.valueOf(object), object.getId());
    }

    /**
     * Stores the entries of a listed row. The entries of the object of the row are stored, and those of an embedded
     * object only if the cache does not hold them yet: the rows of a listing share their workspace, namespace, store
     * and styles, and storing them again would compress their JSON again.
     */
    private void putListedEntries(List<CacheEntry> entries, long epochSeen) {
        if (entries.isEmpty()) {
            return;
        }
        String rowId = entries.getFirst().key().key();
        for (CacheEntry entry : entries) {
            if (isEntryOf(entry, rowId) || !cache.contains(entry.key())) {
                putFromListing(entry, epochSeen);
            }
        }
    }

    // The key of an entry holding JSON is the id of its object
    private static boolean isEntryOf(CacheEntry entry, String id) {
        if (entry.key().holdsJson()) {
            return id.equals(entry.key().key());
        }
        return id.equals(entry.id());
    }

    private void putFromListing(CacheEntry entry, long epochSeen) {
        if (entry.key().holdsJson()) {
            cache.putFromListing(entry.key(), entry.json(), epochSeen);
        } else {
            cache.putIdFromListing(entry.key(), entry.id(), epochSeen);
        }
    }

    /** Evicts the keys made stale by a write, once the write commits. */
    void evictAfterWrite(Collection<CacheKey> keys) {
        access.evictAfterWrite(keys);
    }

    /**
     * Evicts the keys made stale by a write, and the keys of other objects made stale by it, once the write commits.
     */
    void evictAfterWrite(Collection<CacheKey> keys, Predicate<CacheKey> dependents) {
        access.evictAfterWrite(keys, dependents);
    }

    /**
     * Evicts the keys at once, as a lookup does with the keys found stale. A write evicts its keys after its statement
     * instead, see {@link #evictAfterWrite(Collection)}.
     */
    void evict(List<CacheKey> keys) {
        cache.evict(keys.toArray(CacheKey[]::new));
    }
}
