/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.backend.pgconfig;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.backend.pgconfig.cache.CacheAccess;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup;
import org.geoserver.cloud.backend.pgconfig.cache.PutBudget;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.CatalogInfoRowMapper;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.LoggingTemplate;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigObjectMapper;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import tools.jackson.databind.ObjectMapper;

/**
 * Stores the GWC settings of tile layers in the {@code tilelayer} column of the rows of their layers and groups.
 *
 * <p>With an {@link EntityCache}, the reads of settings use the settings kept next to the cached layers and groups: the
 * catalog lookups of the objects put them there, from the same rows. A tile request then finds its settings without a
 * query of its own. The writes evict the cached settings.
 *
 * @since 1.7
 */
@Slf4j
public class PgconfigTileLayerInfoRepository implements TileLayerInfoRepository {

    private static final String TILE_LAYERS_TABLE = "tilelayers_mat";

    /** Encodes the settings to JSON, and decodes them. */
    private static final ObjectMapper SETTINGS_MAPPER = PgconfigObjectMapper.newObjectMapper();

    private final LoggingTemplate template;

    /** The entity cache of the catalog, null without one. */
    private final @Nullable EntityCache cache;

    /** When the reads may use {@link #cache}, and when the evictions of the writes run. Null without a cache. */
    private final @Nullable CacheAccess access;

    /**
     * @param template runs the statements on the database of the catalog
     * @param cache the entity cache of the catalog, or null without one: each lookup then queries the database
     */
    public PgconfigTileLayerInfoRepository(@NonNull JdbcTemplate template, @Nullable EntityCache cache) {
        this.template = new LoggingTemplate(template);
        this.cache = cache;
        this.access = null == cache ? null : new CacheAccess(cache, template.getDataSource());
    }

    /**
     * Returns the GWC settings of a layer or group, decoded for this call.
     *
     * <p>With the cache in use, the settings come from their cache entry. The entry may be absent although the cache
     * holds the layer or group: the listings of the catalog put objects without their settings, and the cache evicts
     * the two entries separately. The query then runs, and puts its answer in the cache.
     */
    @Override
    public Optional<TileLayerInfo> findSettings(@NonNull String publishedId) throws DataAccessException {
        Optional<byte[]> json = readSettings(publishedId);
        return json.flatMap(PgconfigTileLayerInfoRepository::decode);
    }

    private Optional<byte[]> readSettings(String publishedId) {
        EntityCache usable = cacheInUse();
        if (null == usable) {
            return querySettings(publishedId);
        }
        return cachedOrQueried(usable, publishedId);
    }

    /** Returns the cache if the reads may use it, see {@link CacheAccess#inUse()}. Null otherwise, and without one. */
    private @Nullable EntityCache cacheInUse() {
        if (null == cache || null == access || !access.inUse()) {
            return null;
        }
        return cache;
    }

    /** Reads the settings from the cache, else from the database, and puts the answer of the query in the cache. */
    private Optional<byte[]> cachedOrQueried(EntityCache usable, String publishedId) {
        CacheKey key = CacheKey.tileLayer(publishedId);
        if (usable.lookup(key) instanceof Lookup.Hit hit) {
            return Optional.of(hit.json());
        }
        long epochSeen = usable.epoch();
        Optional<byte[]> queried = querySettings(publishedId);
        queried.ifPresent(json -> usable.put(key, json, epochSeen));
        return queried;
    }

    /**
     * Reads the settings of a layer or group like the catalog lookups, see
     * {@link CatalogInfoRowMapper#TILE_LAYER_COLUMN}. Empty if no layer or group has the id. An empty array if the
     * object has no tile layer, as the cache keeps it.
     */
    private Optional<byte[]> querySettings(String publishedId) {
        String sql = "SELECT %s FROM publishedinfos_mat WHERE id = ?".formatted(CatalogInfoRowMapper.TILE_LAYER_COLUMN);
        List<byte[]> rows = template.query(sql, (rs, _) -> utf8OrEmpty(rs.getString(1)), publishedId);
        return rows.stream().findFirst();
    }

    /**
     * Returns the GWC settings of several layers and groups, decoded for this call.
     *
     * <p>With the cache in use, the settings come from their cache entries, and one query reads the others. The query
     * puts its answers in the cache until the {@link PutBudget} bound to the thread is spent. Without a bound budget,
     * it puts them all.
     */
    @Override
    public Map<String, TileLayerInfo> findAllSettings(@NonNull Collection<String> publishedIds)
            throws DataAccessException {
        Map<String, byte[]> json = readAllSettings(publishedIds);
        return decodeAll(json);
    }

    private Map<String, byte[]> readAllSettings(Collection<String> publishedIds) {
        EntityCache usable = cacheInUse();
        if (null == usable) {
            return queryAllSettings(publishedIds);
        }
        return cachedOrQueriedAll(usable, publishedIds);
    }

    /** Reads the settings from the cache, and the settings absent from it with one query. */
    private Map<String, byte[]> cachedOrQueriedAll(EntityCache usable, Collection<String> publishedIds) {
        Map<String, byte[]> found = new HashMap<>();
        List<String> absent = new ArrayList<>();
        for (String publishedId : publishedIds) {
            if (usable.lookup(CacheKey.tileLayer(publishedId)) instanceof Lookup.Hit hit) {
                found.put(publishedId, hit.json());
            } else {
                absent.add(publishedId);
            }
        }
        found.putAll(queryAllAndPut(usable, absent));
        return found;
    }

    /** Queries the settings, and puts each answer in the cache while the budget bound to the thread allows. */
    private Map<String, byte[]> queryAllAndPut(EntityCache usable, List<String> publishedIds) {
        long epochSeen = usable.epoch();
        Map<String, byte[]> queried = queryAllSettings(publishedIds);
        PutBudget budget = PutBudget.current().orElseGet(PutBudget::unlimited);
        queried.forEach((publishedId, json) -> putWithinBudget(usable, budget, publishedId, json, epochSeen));
        return queried;
    }

    /** Puts the settings of a layer or group in the cache unless the budget is spent, and charges the stored weight. */
    private static void putWithinBudget(
            EntityCache usable, PutBudget budget, String publishedId, byte[] json, long epochSeen) {
        if (budget.isSpent()) {
            return;
        }
        long weight = usable.put(CacheKey.tileLayer(publishedId), json, epochSeen);
        budget.charge(weight);
    }

    /**
     * Reads the settings of layers and groups with one query, by id, like {@link #querySettings(String)}. An id without
     * layer or group has no entry. An empty array stands for an object without tile layer, as the cache keeps it. No
     * query runs without ids.
     */
    private Map<String, byte[]> queryAllSettings(Collection<String> publishedIds) {
        if (publishedIds.isEmpty()) {
            return Map.of();
        }
        String sql = "SELECT id, %s FROM publishedinfos_mat WHERE id = ANY(?)"
                .formatted(CatalogInfoRowMapper.TILE_LAYER_COLUMN);
        ResultSetExtractor<Map<String, byte[]>> settingsById = PgconfigTileLayerInfoRepository::settingsById;
        // one parameter holding the array: varargs would spread a String[] into one parameter per id
        Object idsParameter = publishedIds.toArray(String[]::new);
        return template.query(sql, settingsById, idsParameter);
    }

    private static Map<String, byte[]> settingsById(ResultSet rs) throws SQLException {
        Map<String, byte[]> settings = new HashMap<>();
        while (rs.next()) {
            settings.put(rs.getString("id"), utf8OrEmpty(rs.getString("tilelayer")));
        }
        return settings;
    }

    private static byte[] utf8OrEmpty(@Nullable String json) {
        if (null == json) {
            return new byte[0];
        }
        return json.getBytes(UTF_8);
    }

    /** Decodes the settings of each id, and leaves out the ids of layers and groups without tile layer. */
    private static Map<String, TileLayerInfo> decodeAll(Map<String, byte[]> jsonById) {
        Map<String, TileLayerInfo> settings = new HashMap<>();
        for (Map.Entry<String, byte[]> json : jsonById.entrySet()) {
            Optional<TileLayerInfo> decoded = decode(json.getValue());
            decoded.ifPresent(found -> settings.put(json.getKey(), found));
        }
        return settings;
    }

    /** Decodes the settings JSON, in UTF-8. An empty array stands for a layer or group without tile layer. */
    private static Optional<TileLayerInfo> decode(byte[] json) {
        if (json.length == 0) {
            return Optional.empty();
        }
        TileLayerInfo settings = SETTINGS_MAPPER.readValue(json, TileLayerInfo.class);
        return Optional.of(settings);
    }

    /** Stores the settings in the row of the layer or group, and evicts the cached settings once the write commits. */
    @Override
    public boolean save(@NonNull String publishedId, @NonNull TileLayerInfo settings) throws DataAccessException {
        String encoded = SETTINGS_MAPPER.writeValueAsString(settings);
        log.debug("Saving TileLayer {}: {}", publishedId, encoded);
        String sql = "UPDATE publishedinfo SET tilelayer = to_json(?::json) WHERE id = ?";
        int updated = template.update(sql, encoded, publishedId);
        evictAfterWrite(publishedId);
        return updated == 1;
    }

    /** Clears the settings in the row of the layer or group, and evicts the cached settings once the write commits. */
    @Override
    public boolean delete(@NonNull String publishedId) throws DataAccessException {
        String sql = "UPDATE publishedinfo SET tilelayer = NULL WHERE id = ? AND tilelayer IS NOT NULL";
        int updated = template.update(sql, publishedId);
        evictAfterWrite(publishedId);
        return updated == 1;
    }

    /**
     * Evicts the cached settings of a layer or group once the write commits. The eviction also runs for a new tile
     * layer: a lookup before the write may have cached an entry saying that the object has none.
     */
    private void evictAfterWrite(String publishedId) {
        if (null != access) {
            access.evictAfterWrite(List.of(CacheKey.tileLayer(publishedId)));
        }
    }

    @Override
    public List<String> findAllIds() throws DataAccessException {
        String query = "SELECT id FROM \"%s\"".formatted(TILE_LAYERS_TABLE);
        return template.queryForList(query, String.class);
    }

    @Override
    public int count() throws DataAccessException {
        String query = "SELECT count(*) FROM \"%s\"".formatted(TILE_LAYERS_TABLE);
        Integer count = template.queryForObject(query, Integer.class);
        return count == null ? 0 : count;
    }

    @Override
    public Set<String> findAllNames() throws DataAccessException {
        String query = "SELECT name FROM \"%s\"".formatted(TILE_LAYERS_TABLE);
        try (Stream<String> stream = template.queryForStream(query, (rs, _) -> rs.getString(1))) {
            return stream.collect(Collectors.toCollection(TreeSet::new));
        }
    }
}
