/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.catalog.repository;

import lombok.Getter;
import lombok.NonNull;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * @since 1.4
 */
public class PgsqlWorkspaceRepository extends PgsqlCatalogInfoRepository<WorkspaceInfo>
        implements WorkspaceRepository {

    private final @Getter Class<WorkspaceInfo> contentType = WorkspaceInfo.class;
    private final @Getter String queryTable = "workspaceinfos";

    /**
     * @param template
     */
    public PgsqlWorkspaceRepository(@NonNull JdbcTemplate template) {
        super(template);
    }

    @Override
    public void unsetDefaultWorkspace() {
        template.update(
                """
                UPDATE workspaceinfo SET default_workspace = FALSE WHERE default_workspace = TRUE
                """);
    }

    /** TODO: handle transactions and perform unset/set atomically */
    @Override
    @Transactional
    public void setDefaultWorkspace(@NonNull WorkspaceInfo workspace) {
        unsetDefaultWorkspace();
        template.update(
                """
                UPDATE workspaceinfo SET default_workspace = TRUE WHERE id = ?
                """,
                workspace.getId());
    }

    @Override
    public Optional<WorkspaceInfo> getDefaultWorkspace() {
        return findOne(
                """
                SELECT workspace FROM workspaceinfos WHERE default_workspace = TRUE
                """);
    }

    @Override
    protected RowMapper<WorkspaceInfo> newRowMapper() {
        return CatalogInfoRowMapper.workspace();
    }
}
