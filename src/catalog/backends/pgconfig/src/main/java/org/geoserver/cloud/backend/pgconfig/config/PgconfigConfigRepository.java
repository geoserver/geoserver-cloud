/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.backend.pgconfig.cache.CacheAccess;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.ConfigInfoKeys;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigObjectMapper;
import org.geoserver.cloud.backend.pgconfig.config.ConfigRepositoryCache.IdLookup;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.plugin.ConfigRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.ObjectMapper;

/**
 * The configuration of GeoServer in PostgreSQL: the global settings, the logging settings, the settings of the
 * workspaces and the services.
 *
 * <p>With an {@link EntityCache}, lookups read the cache first, and their queries feed it. A write evicts the keys made
 * stale by it. The cache side of these operations is in {@link ConfigRepositoryCache}.
 *
 * @since 1.4
 */
@RequiredArgsConstructor
public class PgconfigConfigRepository implements ConfigRepository {

    private final @NonNull JdbcTemplate template;
    protected static final ObjectMapper infoMapper = PgconfigObjectMapper.newObjectMapper();

    private static final RowMapper<String> InfoJsonRowMapper = (rs, _) -> rs.getString("info");

    private static final RowMapper<ServiceInfo> ServiceInfoRowMapper =
            (rs, _) -> decode(rs.getString("info"), ServiceInfo.class);

    private @Nullable ConfigRepositoryCache cache;

    /**
     * Sets the cache of the configuration objects read by this repository. Null, the default, reads the objects from
     * the database.
     */
    public void setEntityCache(@Nullable EntityCache entityCache) {
        if (null == entityCache) {
            this.cache = null;
        } else {
            this.cache = new ConfigRepositoryCache(entityCache, template.getDataSource(), infoMapper);
        }
    }

    @Override
    public Optional<GeoServerInfo> getGlobal() {
        return find(ConfigInfoKeys.global(), GeoServerInfo.class, this::queryGlobal);
    }

    @Override
    public void setGlobal(GeoServerInfo global) {
        String value = encode(global);
        // the database decides between an update and an insert: a stale miss in the cache would insert a second row
        if (queryGlobal().isPresent()) {
            template.update("UPDATE geoserverinfo SET info = to_json(?::json)", value);
        } else {
            template.update("INSERT INTO geoserverinfo(info) VALUES (to_json(?::json))", value);
        }
        evictAfterWrite(List.of(ConfigInfoKeys.global()));
    }

    private Optional<String> queryGlobal() {
        return queryInfo("""
                SELECT info FROM geoserverinfo LIMIT 1
                """);
    }

    @Override
    public Optional<SettingsInfo> getSettingsByWorkspace(WorkspaceInfo workspace) {
        String query =
                """
                SELECT info, workspace FROM settingsinfos WHERE "workspace.id" = ?
                """;
        String workspaceId = workspace.getId();
        CacheKey key = ConfigInfoKeys.settingsByWorkspace(workspaceId);
        Predicate<SettingsInfo> ofWorkspace = settings -> workspaceId.equals(idOf(settings.getWorkspace()));
        return findThrough(settingsLookup(key, ofWorkspace), () -> queryInfo(query, workspaceId));
    }

    @Override
    public Optional<SettingsInfo> getSettingsById(String id) {
        String query = """
                SELECT info, workspace FROM settingsinfos WHERE id = ?
                """;
        return find(ConfigInfoKeys.settings(id), SettingsInfo.class, () -> queryInfo(query, id));
    }

    @Override
    public void add(SettingsInfo settings) {
        String value = encode(settings);
        template.update("INSERT INTO settingsinfo(info) VALUES (to_json(?::json))", value);
        // a miss stored for the id or the workspace of the settings would hide them
        evictAfterWrite(ConfigInfoKeys.keysOf(settings));
    }

    @Override
    public SettingsInfo update(SettingsInfo settings, Patch patch) {
        return update(settings, patch, SettingsInfo.class, "settingsinfo", "settingsinfos", ConfigInfoKeys::keysOf);
    }

    /** Updates an object, and evicts its keys before and after the patch: a patch may change its name or workspace. */
    @SuppressWarnings("java:S2077") // table is a safe argument, constrained to "settingsinfo" and "serviceinfo"
    private <T extends Info> T update(
            T value, Patch patch, Class<T> clazz, String table, String querytable, Function<T, List<CacheKey>> keysOf) {

        String id = value.getId();
        T current = findCurrent(id, clazz, querytable);
        List<CacheKey> staleKeys = new ArrayList<>(keysOf.apply(current));
        T patched = patch.applyTo(current);
        staleKeys.addAll(keysOf.apply(patched));

        String encoded = encode(patched);
        template.update(
                """
                UPDATE %s SET info = to_json(?::json) WHERE id = ?
                """
                        .formatted(table),
                encoded,
                id);
        evictAfterWrite(staleKeys);
        return patched;
    }

    /**
     * Reads an object from the database, without the cache. An update applies its patch to this copy and writes it
     * back: a copy from the cache could miss a change made by another service instance, and the update would undo it.
     */
    @SuppressWarnings("java:S2077") // queryTable is a safe argument, constrained to "settingsinfos" and "serviceinfos"
    private <T extends Info> T findCurrent(String id, Class<T> clazz, String queryTable) {
        String query = """
                SELECT info, workspace FROM %s WHERE id = ?
                """
                .formatted(queryTable);
        Optional<T> found = queryInfo(query, id).map(json -> decode(json, clazz));
        return found.orElseThrow(
                () -> new NoSuchElementException("%s with id %s does not exist".formatted(clazz.getSimpleName(), id)));
    }

    @Override
    public void remove(SettingsInfo settings) {
        template.update("DELETE FROM settingsinfo WHERE id = ?", settings.getId());
        evictAfterWrite(ConfigInfoKeys.keysOf(settings));
    }

    @Override
    public Optional<LoggingInfo> getLogging() {
        String query = """
                SELECT info FROM logginginfo LIMIT 1
                """;
        return find(ConfigInfoKeys.logging(), LoggingInfo.class, () -> queryInfo(query));
    }

    @Override
    public void setLogging(LoggingInfo logging) {
        String value = encode(logging);
        template.update(
                """
                DELETE FROM logginginfo;
                INSERT INTO logginginfo(info) VALUES(to_json(?::json))
                """,
                value);
        evictAfterWrite(List.of(ConfigInfoKeys.logging()));
    }

    @Override
    public void add(ServiceInfo service) {
        String value = encode(service);
        template.update("INSERT INTO serviceinfo(info) VALUES(to_json(?::json))", value);
        // a miss stored for the id, a class or the name of the service would hide it
        evictAfterWrite(ConfigInfoKeys.keysOf(service));
    }

    @Override
    public void remove(ServiceInfo service) {
        template.update("DELETE FROM serviceinfo WHERE id = ?", service.getId());
        evictAfterWrite(ConfigInfoKeys.keysOf(service));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S extends ServiceInfo> S update(S service, Patch patch) {
        return (S) update(service, patch, ServiceInfo.class, "serviceinfo", "serviceinfos", ConfigInfoKeys::keysOf);
    }

    @Override
    public Stream<ServiceInfo> getGlobalServices() {
        return template.queryForStream(
                """
                SELECT info, workspace FROM serviceinfos WHERE "workspace.id" IS NULL
                """,
                listingRowMapper());
    }

    @Override
    public Stream<ServiceInfo> getServicesByWorkspace(WorkspaceInfo workspace) {
        String workspaceId = workspace.getId();
        return template.queryForStream(
                """
                SELECT info, workspace FROM serviceinfos WHERE "workspace.id" = ?
                """,
                listingRowMapper(),
                workspaceId);
    }

    /**
     * The row mapper of the listings of services. With the cache in use, it also puts the services in the cache: the
     * services are few, and their listings feed the cache whatever {@code listing-write-through} says.
     */
    private RowMapper<ServiceInfo> listingRowMapper() {
        ConfigRepositoryCache listingCache = cache;
        if (null == listingCache || !listingCache.inUse()) {
            return ServiceInfoRowMapper;
        }
        long epochSeen = listingCache.epoch();
        return (rs, _) ->
                listingCache.readListed(rs.getString("info"), ServiceInfo.class, ConfigInfoKeys::service, epochSeen);
    }

    @Override
    public <T extends ServiceInfo> Optional<T> getGlobalService(Class<T> clazz) {
        CacheKey key = ConfigInfoKeys.serviceByWorkspaceAndClass(null, clazz);
        return findServiceByClass(
                key, clazz, inWorkspace(null), """
                "workspace.id" IS NULL
                """);
    }

    @Override
    public <T extends ServiceInfo> Optional<T> getServiceByWorkspace(WorkspaceInfo workspace, Class<T> clazz) {
        String workspaceId = workspace.getId();
        CacheKey key = ConfigInfoKeys.serviceByWorkspaceAndClass(workspaceId, clazz);
        return findServiceByClass(
                key,
                clazz,
                inWorkspace(workspaceId),
                """
                "workspace.id" = ?
                """,
                workspaceId);
    }

    /**
     * Finds a service by id. The query does not filter by class: the cache then holds the service, or a miss, for any
     * class asked.
     */
    @Override
    public <T extends ServiceInfo> Optional<T> getServiceById(String id, Class<T> clazz) {
        String query = """
                SELECT info, workspace FROM serviceinfos WHERE id = ?
                """;
        Optional<ServiceInfo> found = find(ConfigInfoKeys.service(id), ServiceInfo.class, () -> queryInfo(query, id));
        return found.filter(clazz::isInstance).map(clazz::cast);
    }

    @Override
    public <T extends ServiceInfo> Optional<T> getServiceByName(String name, Class<T> clazz) {
        CacheKey key = ConfigInfoKeys.serviceByName(name);
        return findServiceByName(key, clazz, named(name), """
                name = ?
                """, name);
    }

    @Override
    public <T extends ServiceInfo> Optional<T> getServiceByNameAndWorkspace(
            String name, WorkspaceInfo workspace, Class<T> clazz) {

        String workspaceId = workspace.getId();
        CacheKey key = ConfigInfoKeys.serviceByNameAndWorkspace(workspaceId, name);
        Predicate<ServiceInfo> namedInWorkspace = named(name).and(inWorkspace(workspaceId));
        return findServiceByName(
                key,
                clazz,
                namedInWorkspace,
                """
                "workspace.id" = ? AND name = ?
                """,
                workspaceId,
                name);
    }

    /**
     * Finds a service under a key naming the class asked and the workspace: an empty answer of the query is the miss of
     * the key.
     */
    private <T extends ServiceInfo> Optional<T> findServiceByClass(
            CacheKey key, Class<T> clazz, Predicate<ServiceInfo> inWorkspace, String whereClause, Object... args) {
        IdLookup<ServiceInfo> lookup = serviceLookup(key, inWorkspace.and(clazz::isInstance), true);
        return findService(lookup, clazz, whereClause, args);
    }

    /**
     * Finds a service under the key of its name. Services of different classes may share a name: the key remembers an
     * empty answer of the query only if the query asks for any class.
     */
    private <T extends ServiceInfo> Optional<T> findServiceByName(
            CacheKey key, Class<T> clazz, Predicate<ServiceInfo> named, String whereClause, Object... args) {
        boolean anyClass = ServiceInfo.class.equals(clazz);
        IdLookup<ServiceInfo> lookup = serviceLookup(key, named.and(clazz::isInstance), anyClass);
        return findService(lookup, clazz, whereClause, args);
    }

    private <T extends ServiceInfo> Optional<T> findService(
            IdLookup<ServiceInfo> lookup, Class<T> clazz, String whereClause, Object... args) {
        String sql = serviceQuery(whereClause, clazz);
        Optional<ServiceInfo> found = findThrough(lookup, () -> queryInfo(sql, args));
        return found.filter(clazz::isInstance).map(clazz::cast);
    }

    private <T extends ServiceInfo> String serviceQuery(String whereClause, Class<T> clazz) {
        String sql = "SELECT info, workspace FROM serviceinfos WHERE %s".formatted(whereClause);
        if (!ServiceInfo.class.equals(clazz)) {
            String servicetype = servicetype(clazz);
            sql = """
                    %s AND "@type" = '%s'
                    """.formatted(sql, servicetype);
        }
        return sql;
    }

    private <S extends ServiceInfo> String servicetype(Class<S> clazz) {
        if (ServiceInfo.class.equals(clazz)) {
            return ServiceInfo.class.getSimpleName();
        }
        Class<?> iface = clazz.isInterface()
                ? clazz
                : Arrays.stream(clazz.getInterfaces())
                        .filter(ServiceInfo.class::isAssignableFrom)
                        .findFirst()
                        .orElseThrow(IllegalArgumentException::new);
        return iface.getSimpleName();
    }

    @Override
    public void dispose() {
        // no-op
    }

    /**
     * A lookup of a service through a key holding its id. A cached service answers it if it matches the predicate: the
     * class asked, and the name or the workspace held by the key.
     */
    private static IdLookup<ServiceInfo> serviceLookup(
            CacheKey key, Predicate<ServiceInfo> answers, boolean remembersMiss) {
        return new IdLookup<>(key, ServiceInfo.class, ConfigInfoKeys::service, answers, remembersMiss);
    }

    private static IdLookup<SettingsInfo> settingsLookup(CacheKey key, Predicate<SettingsInfo> answers) {
        return new IdLookup<>(key, SettingsInfo.class, ConfigInfoKeys::settings, answers, true);
    }

    /** Matches the services of a workspace, or the global services for a null id. */
    private static Predicate<ServiceInfo> inWorkspace(@Nullable String workspaceId) {
        return service -> Objects.equals(workspaceId, idOf(service.getWorkspace()));
    }

    private static Predicate<ServiceInfo> named(String name) {
        return service -> name.equals(service.getName());
    }

    // An unresolved workspace is a proxy, and still tells its id
    private static @Nullable String idOf(@Nullable WorkspaceInfo workspace) {
        return null == workspace ? null : workspace.getId();
    }

    /** Finds an object by its {@code ENTITY} key, through the cache when it is in use. */
    private <T extends Info> Optional<T> find(CacheKey key, Class<T> type, Supplier<Optional<String>> query) {
        if (cacheInUse()) {
            return cache.find(key, type, query);
        }
        return query.get().map(json -> decode(json, type));
    }

    /** Finds an object through the {@code LOOKUP} key of a query, through the cache when it is in use. */
    private <T extends Info> Optional<T> findThrough(IdLookup<T> lookup, Supplier<Optional<String>> query) {
        if (cacheInUse()) {
            return cache.findThrough(lookup, query);
        }
        return query.get().map(json -> decode(json, lookup.type()));
    }

    /**
     * Whether lookups go through the cache: there is one, it is not suspended, and no transaction runs on the data
     * source, see {@link CacheAccess#inUse()}.
     */
    private boolean cacheInUse() {
        return null != cache && cache.inUse();
    }

    /** Evicts the keys made stale by a write, once the write commits. */
    private void evictAfterWrite(List<CacheKey> keys) {
        if (null != cache) {
            cache.evictAfterWrite(keys);
        }
    }

    /** Returns the JSON of the object found by a query, or empty if the query finds no row. */
    private Optional<String> queryInfo(String query, Object... args) {
        try {
            String json = template.queryForObject(query, InfoJsonRowMapper, args);
            return Optional.ofNullable(json);
        } catch (EmptyResultDataAccessException _) {
            return Optional.empty();
        }
    }

    private String encode(Info info) {
        return infoMapper.writeValueAsString(info);
    }

    private static <C extends Info> C decode(String value, Class<C> type) {
        return infoMapper.readValue(value, type);
    }

    private static @NonNull String infoType(Class<? extends CatalogInfo> clazz) {
        ClassMappings cm;
        if (clazz.isInterface()) {
            cm = ClassMappings.fromInterface(clazz);
        } else {
            cm = ClassMappings.fromImpl(clazz);
        }

        return cm.getInterface().getSimpleName();
    }
}
