/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import java.util.Optional;
import lombok.NonNull;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @since 1.4
 */
public class PgconfigWorkspaceRepository extends PgconfigCatalogInfoRepository<WorkspaceInfo>
        implements WorkspaceRepository {

    private static final String UNSET_DEFAULT_WORKSPACE =
            """
            UPDATE workspaceinfo SET default_workspace = FALSE WHERE default_workspace = TRUE
            """;

    /**
     * @param template
     */
    public PgconfigWorkspaceRepository(@NonNull JdbcTemplate template) {
        super(WorkspaceInfo.class, template);
    }

    @Override
    protected String getQueryTable() {
        return "workspaceinfos";
    }

    @Override
    protected String getReturnColumns() {
        return CatalogInfoRowMapper.WORKSPACE_BUILD_COLUMNS;
    }

    @Override
    public void unsetDefaultWorkspace() {
        template.update(UNSET_DEFAULT_WORKSPACE);
    }

    /** TODO: handle transactions and perform unset/set atomically */
    @Override
    @Transactional
    public void setDefaultWorkspace(@NonNull WorkspaceInfo workspace) {
        unsetDefaultWorkspace();
        template.update(
                """
                UPDATE %s SET default_workspace = TRUE WHERE id = ?
                """
                        .formatted(getUpdateTable()),
                workspace.getId());
    }

    @Override
    public Optional<WorkspaceInfo> getDefaultWorkspace() {
        return findOne(select("WHERE default_workspace = TRUE"));
    }
}
