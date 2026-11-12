/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.CatalogInfoKeys;
import org.springframework.jdbc.core.JdbcTemplate;

/** @since 1.4 */
public class PgconfigStyleRepository extends PgconfigCatalogInfoRepository<StyleInfo> implements StyleRepository {

    /** @param template */
    public PgconfigStyleRepository(@NonNull JdbcTemplate template) {
        super(StyleInfo.class, template);
    }

    public PgconfigStyleRepository(@NonNull LoggingTemplate template) {
        super(StyleInfo.class, template);
    }

    @Override
    protected String getQueryTable() {
        return "styleinfos";
    }

    @Override
    protected String getReturnColumns() {
        return CatalogInfoRowMapper.STYLEINFO_BUILD_COLUMNS;
    }

    @Override
    public Stream<StyleInfo> findAllByNullWorkspace() {
        return queryForStream(select("WHERE \"workspace.id\" IS NULL"));
    }

    @Override
    public Stream<StyleInfo> findAllByWorkspace(@NonNull WorkspaceInfo ws) {
        return queryForStream(select("WHERE \"workspace.id\" = ?"), ws.getId());
    }

    @Override
    public Optional<StyleInfo> findByNameAndWorkspaceIsNull(@NonNull String name) {
        List<CacheKey> keys = globalNameKeys(name, StyleInfo.class);
        return findByKeys(
                keys, StyleInfo.class, () -> findOne(select("WHERE \"workspace.id\" IS NULL AND name = ?"), name));
    }

    @Override
    public Optional<StyleInfo> findByNameAndWorkspace(@NonNull String name, @NonNull WorkspaceInfo workspace) {
        String query = select("WHERE \"workspace.id\" = ? AND name = ?");
        List<CacheKey> keys = nameKeys(CatalogInfoKeys.prefixedName(workspace, name), StyleInfo.class);
        return findByKeys(keys, StyleInfo.class, () -> findOne(query, workspace.getId(), name));
    }
}
