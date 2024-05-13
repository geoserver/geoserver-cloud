/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import lombok.NonNull;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * @since 1.4
 */
public class PgconfigStoreRepository extends PgconfigCatalogInfoRepository<StoreInfo>
        implements StoreRepository {

    /**
     * @param template
     */
    public PgconfigStoreRepository(@NonNull JdbcTemplate template) {
        super(template);
    }

    @Override
    public Class<StoreInfo> getContentType() {
        return StoreInfo.class;
    }

    @Override
    protected String getQueryTable() {
        return "storeinfos";
    }

    @Override
    public <U extends StoreInfo> Optional<U> findById(@NonNull String id, Class<U> clazz) {
        String sql =
                """
                SELECT store, workspace
                FROM storeinfos
                WHERE id = ?
                """;
        return findOne(sql, clazz, id);
    }

    @Override
    public void setDefaultDataStore(
            @NonNull WorkspaceInfo workspace, @NonNull DataStoreInfo dataStore) {
        String sql = "UPDATE workspaceinfo SET default_store = ? WHERE id = ?";
        template.update(sql, dataStore.getId(), workspace.getId());
    }

    @Override
    public void unsetDefaultDataStore(@NonNull WorkspaceInfo workspace) {
        String sql = "UPDATE workspaceinfo SET default_store = NULL WHERE id = ?";
        template.update(sql, workspace.getId());
    }

    @Override
    public Optional<DataStoreInfo> getDefaultDataStore(@NonNull WorkspaceInfo workspace) {
        String sql =
                """
                SELECT store, workspace
                FROM storeinfos
                WHERE id = (SELECT default_store FROM workspaceinfo WHERE id = ?)
                """;
        return findOne(sql, DataStoreInfo.class, workspace.getId());
    }

    @Override
    public Stream<DataStoreInfo> getDefaultDataStores() {
        String sql =
                """
                SELECT s.store, s.workspace
                FROM storeinfos s
                INNER JOIN workspaceinfo w ON s."workspace.id" = w.id AND s.id = w.default_store;
                """;
        return super.queryForStream(DataStoreInfo.class, sql);
    }

    @Override
    public <U extends StoreInfo> Stream<U> findAllByWorkspace(
            @NonNull WorkspaceInfo workspace, @NonNull Class<U> clazz) {

        String sql =
                """
                SELECT store, workspace
                FROM storeinfos
                WHERE "workspace.id" = ?
                """;

        final String workspaceId = workspace.getId();
        if (StoreInfo.class.equals(clazz)) {
            return super.queryForStream(clazz, sql, workspaceId);
        }

        String infotype = infoType(clazz);
        sql += " AND \"@type\" = ?::infotype";
        return super.queryForStream(clazz, sql, workspaceId, infotype);
    }

    @Override
    public <T extends StoreInfo> Stream<T> findAllByType(@NonNull Class<T> clazz) {

        if (StoreInfo.class.equals(clazz)) {
            return super.queryForStream(clazz, "SELECT store, workspace FROM storeinfos");
        }

        String infotype = infoType(clazz);
        return super.queryForStream(
                clazz,
                "SELECT store, workspace FROM storeinfos WHERE \"@type\" = ?::infotype",
                infotype);
    }

    @Override
    public <T extends StoreInfo> Optional<T> findByNameAndWorkspace(
            @NonNull String name, @NonNull WorkspaceInfo workspace, @NonNull Class<T> clazz) {

        return findOne(
                """
                SELECT store, workspace FROM storeinfos WHERE "workspace.id" = ? AND name = ?
                """,
                clazz,
                workspace.getId(),
                name);
    }

    @Override
    protected RowMapper<StoreInfo> newRowMapper() {
        return CatalogInfoRowMapper.store();
    }
}
