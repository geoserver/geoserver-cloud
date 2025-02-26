/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * @since 1.4
 */
public class PgconfigLayerGroupRepository extends PgconfigPublishedInfoRepository<LayerGroupInfo>
        implements LayerGroupRepository {

    /**
     * @param template
     */
    public PgconfigLayerGroupRepository(@NonNull JdbcTemplate template, @NonNull PgconfigStyleRepository styleLoader) {
        super(LayerGroupInfo.class, template, styleLoader);
    }

    @Override
    protected String getReturnColumns() {
        return CatalogInfoRowMapper.LAYERGROUPINFO_BUILD_COLUMNS;
    }

    @Override
    protected final RowMapper<LayerGroupInfo> newRowMapper() {
        return CatalogInfoRowMapper.<LayerGroupInfo>newInstance().setStyleLoader(styleRepo::findById);
    }

    @Override
    public Optional<LayerGroupInfo> findByNameAndWorkspaceIsNull(@NonNull String name) {
        String sql =
                """
                SELECT "@type", publishedinfo, workspace \
                FROM %s \
                WHERE "@type" = 'LayerGroupInfo' AND "workspace.id" IS NULL AND name = ?
                """
                        .formatted(getQueryTable());
        return findOne(sql, name);
    }

    @Override
    public Optional<LayerGroupInfo> findByNameAndWorkspace(@NonNull String name, @NonNull WorkspaceInfo workspace) {

        String sql =
                """
                SELECT "@type", publishedinfo, workspace \
                FROM %s \
                WHERE "@type" = 'LayerGroupInfo' AND "workspace.id" = ? AND name = ?
                """
                        .formatted(getQueryTable());
        return findOne(sql, workspace.getId(), name);
    }

    @Override
    public Stream<LayerGroupInfo> findAllByWorkspaceIsNull() {
        String sql =
                """
                SELECT "@type", publishedinfo, workspace \
                FROM %s \
                WHERE "@type" = 'LayerGroupInfo' AND "workspace.id" IS NULL
                """
                        .formatted(getQueryTable());
        return super.queryForStream(sql);
    }

    @Override
    public Stream<LayerGroupInfo> findAllByWorkspace(WorkspaceInfo workspace) {
        String sql =
                """
                SELECT "@type", publishedinfo, workspace \
                FROM %s \
                WHERE "@type" = 'LayerGroupInfo' AND "workspace.id" = ?
                """
                        .formatted(getQueryTable());
        return super.queryForStream(sql, workspace.getId());
    }
}
