/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.CatalogInfoKeys;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** @since 1.4 */
public class PgconfigWorkspaceRepository extends PgconfigCatalogInfoRepository<WorkspaceInfo>
        implements WorkspaceRepository {

    private static final String UNSET_DEFAULT_WORKSPACE =
            """
            UPDATE workspaceinfo SET default_workspace = FALSE WHERE default_workspace = TRUE
            """;

    /** @param template */
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

    /** The name of a workspace is unique, and is the key of its {@code NAME} entry. */
    @Override
    public <U extends WorkspaceInfo> Optional<U> findFirstByName(@NonNull String name, Class<U> clazz) {
        List<CacheKey> keys = nameKeys(name, clazz);
        return findByKeys(keys, clazz, () -> super.findFirstByName(name, clazz));
    }

    @Override
    public void unsetDefaultWorkspace() {
        template.update(UNSET_DEFAULT_WORKSPACE);
        evictAfterWrite(CatalogInfoKeys.defaultWorkspace());
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
        evictAfterWrite(CatalogInfoKeys.defaultWorkspace());
    }

    @Override
    public Optional<WorkspaceInfo> getDefaultWorkspace() {
        List<CacheKey> keys = List.of(CatalogInfoKeys.defaultWorkspace());
        return findByKeys(keys, WorkspaceInfo.class, () -> findOne(select("WHERE default_workspace = TRUE")));
    }
}
