/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.catalog.repository;

import lombok.Getter;
import lombok.NonNull;

import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * @since 1.4
 */
public class PgsqlStyleRepository extends PgsqlCatalogInfoRepository<StyleInfo>
        implements StyleRepository {

    private final @Getter Class<StyleInfo> contentType = StyleInfo.class;
    private final @Getter String queryTable = "styleinfos";

    /**
     * @param template
     */
    public PgsqlStyleRepository(@NonNull JdbcTemplate template) {
        super(template);
    }

    @Override
    public Stream<StyleInfo> findAllByNullWorkspace() {
        String query =
                """
                SELECT style, workspace
                FROM styleinfos
                WHERE "workspace.id" IS NULL
                """;
        return template.queryForStream(query, newRowMapper());
    }

    @Override
    public Stream<StyleInfo> findAllByWorkspace(@NonNull WorkspaceInfo ws) {
        String query =
                """
                SELECT style, workspace
                FROM styleinfos
                WHERE "workspace.id" = ?
                """;
        return template.queryForStream(query, newRowMapper(), ws.getId());
    }

    @Override
    public Optional<StyleInfo> findByNameAndWordkspaceNull(@NonNull String name) {
        String query =
                """
                SELECT style, workspace
                FROM styleinfos
                WHERE "workspace.id" IS NULL AND name = ?
                """;
        return findOne(query, StyleInfo.class, newRowMapper(), name);
    }

    @Override
    public Optional<StyleInfo> findByNameAndWorkspace(
            @NonNull String name, @NonNull WorkspaceInfo workspace) {

        String query =
                """
                SELECT style, workspace
                FROM styleinfos
                WHERE "workspace.id" = ? AND name = ?
                """;
        return findOne(query, StyleInfo.class, newRowMapper(), workspace.getId(), name);
    }

    @Override
    protected RowMapper<StyleInfo> newRowMapper() {
        return CatalogInfoRowMapper.style();
    }
}
