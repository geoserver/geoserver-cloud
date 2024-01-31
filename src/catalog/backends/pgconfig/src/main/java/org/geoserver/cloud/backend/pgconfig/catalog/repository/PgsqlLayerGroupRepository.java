/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import lombok.NonNull;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * @since 1.4
 */
public class PgsqlLayerGroupRepository extends PgsqlCatalogInfoRepository<LayerGroupInfo>
        implements LayerGroupRepository {

    /**
     * @param template
     */
    public PgsqlLayerGroupRepository(@NonNull JdbcTemplate template) {
        super(template);
    }

    @Override
    public Class<LayerGroupInfo> getContentType() {
        return LayerGroupInfo.class;
    }

    @Override
    protected String getQueryTable() {
        return "layergroupinfos";
    }

    @Override
    public Optional<LayerGroupInfo> findByNameAndWorkspaceIsNull(@NonNull String name) {
        String sql =
                """
                SELECT publishedinfo, workspace
                FROM layergroupinfos
                WHERE "workspace.id" IS NULL AND name = ?
                """;
        return findOne(sql, LayerGroupInfo.class, newRowMapper(), name);
    }

    @Override
    public Optional<LayerGroupInfo> findByNameAndWorkspace(
            @NonNull String name, @NonNull WorkspaceInfo workspace) {

        String sql =
                """
                SELECT publishedinfo, workspace
                FROM layergroupinfos
                WHERE "workspace.id" = ? AND name = ?
                """;
        return findOne(sql, LayerGroupInfo.class, newRowMapper(), workspace.getId(), name);
    }

    @Override
    public Stream<LayerGroupInfo> findAllByWorkspaceIsNull() {
        String sql =
                """
                SELECT publishedinfo, workspace
                FROM layergroupinfos
                WHERE "workspace.id" IS NULL
                """;
        return template.queryForStream(sql, newRowMapper());
    }

    @Override
    public Stream<LayerGroupInfo> findAllByWorkspace(WorkspaceInfo workspace) {
        String sql =
                """
                SELECT publishedinfo, workspace
                FROM layergroupinfos
                WHERE "workspace.id" = ?
                """;
        return template.queryForStream(sql, newRowMapper(), workspace.getId());
    }

    @Override
    protected RowMapper<LayerGroupInfo> newRowMapper() {
        PgsqlStyleRepository styleLoader = new PgsqlStyleRepository(template);
        return CatalogInfoRowMapper.layerGroup(styleLoader::findById);
    }
}
