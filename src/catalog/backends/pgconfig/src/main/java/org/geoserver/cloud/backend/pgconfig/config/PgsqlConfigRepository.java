/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.plugin.ConfigRepository;
import org.geotools.jackson.databind.util.ObjectMapperUtil;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @since 1.4
 */
@RequiredArgsConstructor
public class PgsqlConfigRepository implements ConfigRepository {

    private final @NonNull JdbcTemplate template;
    protected static final ObjectMapper infoMapper = ObjectMapperUtil.newObjectMapper();

    private static final RowMapper<GeoServerInfo> GeoServerInfoRowMapper =
            (rs, rn) -> decode(rs.getString("info"), GeoServerInfo.class);

    private static final RowMapper<SettingsInfo> SettingsInfoRowMapper =
            (rs, rn) -> decode(rs.getString("info"), SettingsInfo.class);

    private static final RowMapper<ServiceInfo> ServiceInfoRowMapper =
            (rs, rn) -> decode(rs.getString("info"), ServiceInfo.class);

    private static final RowMapper<LoggingInfo> LoggingInfoRowMapper =
            (rs, rn) -> decode(rs.getString("info"), LoggingInfo.class);

    @Override
    public Optional<GeoServerInfo> getGlobal() {
        String sql = """
                SELECT info FROM geoserverinfo LIMIT 1
                """;
        return findOne(sql, GeoServerInfo.class, GeoServerInfoRowMapper);
    }

    @Override
    public void setGlobal(GeoServerInfo global) {
        String value = encode(global);
        getGlobal()
                .ifPresentOrElse(
                        g ->
                                template.update(
                                        "UPDATE geoserverinfo SET info = to_json(?::json)", value),
                        () ->
                                template.update(
                                        "INSERT INTO geoserverinfo(info) VALUES (to_json(?::json))",
                                        value));
    }

    @Override
    public Optional<SettingsInfo> getSettingsByWorkspace(WorkspaceInfo workspace) {
        String query =
                """
                SELECT info, workspace FROM settingsinfos WHERE "workspace.id" = ?
                """;
        String workspaceId = workspace.getId();
        return findOne(query, SettingsInfo.class, SettingsInfoRowMapper, workspaceId);
    }

    @Override
    public Optional<SettingsInfo> getSettingsById(String id) {
        return findById(id, SettingsInfo.class, "settingsinfos", SettingsInfoRowMapper);
    }

    protected <T> Optional<T> findById(
            String id, Class<T> clazz, String queryTable, RowMapper<T> mapper) {
        String query =
                """
                SELECT info, workspace FROM %s WHERE id = ?
                """
                        .formatted(queryTable);
        return findOne(query, clazz, mapper, id);
    }

    @Override
    public void add(SettingsInfo settings) {
        String value = encode(settings);
        template.update("INSERT INTO settingsinfo(info) VALUES (to_json(?::json))", value);
    }

    @Override
    public SettingsInfo update(SettingsInfo settings, Patch patch) {
        return update(
                settings,
                patch,
                SettingsInfo.class,
                "settingsinfo",
                "settingsinfos",
                SettingsInfoRowMapper);
    }

    private <T extends Info> T update(
            T value,
            Patch patch,
            Class<T> clazz,
            String table,
            String querytable,
            RowMapper<T> mapper) {

        String id = value.getId();
        Optional<T> found = findById(id, clazz, querytable, mapper);
        T patched =
                found.map(patch::applyTo)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "%s with id %s does not exist"
                                                        .formatted(clazz.getSimpleName(), id)));

        String encoded = encode(patched);
        template.update(
                """
                UPDATE %s SET info = to_json(?::json) WHERE id = ?
                """
                        .formatted(table),
                encoded,
                id);

        return patched;
    }

    @Override
    public void remove(SettingsInfo settings) {
        template.update("DELETE FROM settingsinfo WHERE id = ?", settings.getId());
    }

    @Override
    public Optional<LoggingInfo> getLogging() {
        String sql = """
                SELECT info FROM logginginfo LIMIT 1
                """;
        return findOne(sql, LoggingInfo.class, LoggingInfoRowMapper);
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
    }

    @Override
    public void add(ServiceInfo service) {
        String value = encode(service);
        template.update("INSERT INTO serviceinfo(info) VALUES(to_json(?::json))", value);
    }

    @Override
    public void remove(ServiceInfo service) {
        template.update("DELETE FROM serviceinfo WHERE id = ?", service.getId());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S extends ServiceInfo> S update(S service, Patch patch) {
        return (S)
                update(
                        service,
                        patch,
                        ServiceInfo.class,
                        "serviceinfo",
                        "serviceinfos",
                        ServiceInfoRowMapper);
    }

    @Override
    public Stream<ServiceInfo> getGlobalServices() {
        return template.queryForStream(
                """
                SELECT info, workspace FROM serviceinfos WHERE "workspace.id" IS NULL
                """,
                ServiceInfoRowMapper);
    }

    @Override
    public Stream<ServiceInfo> getServicesByWorkspace(WorkspaceInfo workspace) {
        String workspaceId = workspace.getId();
        return template.queryForStream(
                """
                SELECT info, workspace FROM serviceinfos WHERE "workspace.id" = ?
                """,
                ServiceInfoRowMapper,
                workspaceId);
    }

    @Override
    public <T extends ServiceInfo> Optional<T> getGlobalService(Class<T> clazz) {
        return findService("""
                "workspace.id" IS NULL
                """, clazz);
    }

    @Override
    public <T extends ServiceInfo> Optional<T> getServiceByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {

        return findService(
                """
                "workspace.id" = ?
                """,
                clazz,
                workspace.getId());
    }

    @Override
    public <T extends ServiceInfo> Optional<T> getServiceById(String id, Class<T> clazz) {
        return findService("""
                id = ?
                """, clazz, id);
    }

    private <T extends ServiceInfo> Optional<T> findService(
            String whereClause, Class<T> clazz, Object... args) {

        String sql = "SELECT info, workspace FROM serviceinfos WHERE %s".formatted(whereClause);
        if (!ServiceInfo.class.equals(clazz)) {
            String servicetype = servicetype(clazz);
            sql =
                    """
                   %s AND "@type" = '%s'
                   """
                            .formatted(sql, servicetype);
        }
        return findOne(sql, ServiceInfo.class, ServiceInfoRowMapper, args)
                .filter(clazz::isInstance)
                .map(clazz::cast);
    }

    private <S extends ServiceInfo> String servicetype(Class<S> clazz) {
        if (ServiceInfo.class.equals(clazz)) return ServiceInfo.class.getSimpleName();
        Class<?> iface =
                clazz.isInterface()
                        ? clazz
                        : Arrays.stream(clazz.getInterfaces())
                                .filter(ServiceInfo.class::isAssignableFrom)
                                .findFirst()
                                .orElseThrow(IllegalArgumentException::new);
        return iface.getSimpleName();
    }

    @Override
    public <T extends ServiceInfo> Optional<T> getServiceByName(String name, Class<T> clazz) {
        return findService("""
                name = ?
                """, clazz, name);
    }

    @Override
    public <T extends ServiceInfo> Optional<T> getServiceByNameAndWorkspace(
            String name, WorkspaceInfo workspace, Class<T> clazz) {

        return findService(
                """
                "workspace.id" = ? AND name = ?
                """,
                clazz,
                workspace.getId(),
                name);
    }

    @Override
    public void dispose() {
        // no-op
    }

    private String encode(Info info) {
        try {
            return infoMapper.writeValueAsString(info);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static <C extends Info> C decode(String value, Class<C> type) {
        try {
            return infoMapper.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static @NonNull String infoType(Class<? extends CatalogInfo> clazz) {
        ClassMappings cm;
        if (clazz.isInterface()) cm = ClassMappings.fromInterface(clazz);
        else cm = ClassMappings.fromImpl(clazz);

        return cm.getInterface().getSimpleName();
    }

    protected <U> Optional<U> findOne(
            @NonNull String query, Class<U> clazz, RowMapper<U> rowMapper, Object... args) {

        try {
            U object = template.queryForObject(query, rowMapper, args);
            return Optional.ofNullable(clazz.isInstance(object) ? clazz.cast(object) : null);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
