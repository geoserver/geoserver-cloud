/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.geoserver.catalog.plugin.resolving.ProxyUtils;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.CatalogInfoKeys;
import org.springframework.jdbc.core.JdbcTemplate;

/** @since 1.4 */
public class PgconfigLayerGroupRepository extends PgconfigPublishedInfoRepository<LayerGroupInfo>
        implements LayerGroupRepository {

    /** @param template */
    public PgconfigLayerGroupRepository(@NonNull JdbcTemplate template, @NonNull PgconfigStyleRepository styleLoader) {
        super(LayerGroupInfo.class, template, styleLoader);
    }

    @Override
    protected String getReturnColumns() {
        return CatalogInfoRowMapper.LAYERGROUPINFO_BUILD_COLUMNS;
    }

    /** The lookups of one group also select its members. */
    @Override
    protected String getLookupColumns() {
        return CatalogInfoRowMapper.LAYERGROUPINFO_WITH_MEMBERS_COLUMNS;
    }

    @Override
    protected final CatalogInfoRowMapper<LayerGroupInfo> newRowMapper() {
        return CatalogInfoRowMapper.<LayerGroupInfo>newInstance().setStyleLoader(styleRepo::findById);
    }

    @Override
    public Optional<LayerGroupInfo> findByNameAndWorkspaceIsNull(@NonNull String name) {
        String predicate =
                """
                WHERE "@type" = 'LayerGroupInfo' AND "workspace.id" IS NULL AND name = ?
                """;
        List<CacheKey> keys = globalNameKeys(name, LayerGroupInfo.class);
        return findByKeys(keys, LayerGroupInfo.class, () -> findOne(selectForLookup(predicate), name));
    }

    @Override
    public Optional<LayerGroupInfo> findByNameAndWorkspace(@NonNull String name, @NonNull WorkspaceInfo workspace) {
        String predicate =
                """
                WHERE "@type" = 'LayerGroupInfo' AND "workspace.id" = ? AND name = ?
                """;
        String prefixedName = CatalogInfoKeys.prefixedName(workspace, name);
        List<CacheKey> keys = nameKeys(prefixedName, LayerGroupInfo.class);
        return findByKeys(
                keys, LayerGroupInfo.class, () -> findOne(selectForLookup(predicate), workspace.getId(), name));
    }

    @Override
    public Stream<LayerGroupInfo> findAllByWorkspaceIsNull() {
        String predicate =
                """
                WHERE "@type" = 'LayerGroupInfo' AND "workspace.id" IS NULL
                """;
        return super.queryForStream(select(predicate));
    }

    @Override
    public Stream<LayerGroupInfo> findAllByWorkspace(WorkspaceInfo workspace) {
        String predicate =
                """
                WHERE "@type" = 'LayerGroupInfo' AND "workspace.id" = ?
                """;
        return super.queryForStream(select(predicate), workspace.getId());
    }

    @Override
    protected String encode(LayerGroupInfo info) {
        if (info != null) {
            // beware default styles may come as ResolvingProxy("") from the rest API instead of null
            List<StyleInfo> styles =
                    info.getStyles().stream().map(this::sanitizeResolvingProxy).toList();
            info.getStyles().clear();
            info.getStyles().addAll(styles);
        }
        return super.encode(info);
    }

    private StyleInfo sanitizeResolvingProxy(StyleInfo s) {
        if (ProxyUtils.isResolvingProxy(s)) {
            String ref = ResolvingProxy.getRef(s);
            if (null == ref) {
                return null;
            }
        }
        return s;
    }
}
