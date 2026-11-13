/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
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
import org.geoserver.cloud.backend.pgconfig.cache.PutBudget;
import org.springframework.jdbc.core.JdbcTemplate;

/** @since 1.4 */
public class PgconfigLayerGroupRepository extends PgconfigPublishedInfoRepository<LayerGroupInfo>
        implements LayerGroupRepository {

    private static final String COLUMNS_WITH_TILE_LAYER =
            CatalogInfoRowMapper.LAYERGROUPINFO_BUILD_COLUMNS + ", " + CatalogInfoRowMapper.TILE_LAYER_COLUMN;

    private static final String COLUMNS_WITH_MEMBERS_AND_TILE_LAYER =
            CatalogInfoRowMapper.LAYERGROUPINFO_WITH_MEMBERS_COLUMNS + ", " + CatalogInfoRowMapper.TILE_LAYER_COLUMN;

    /** The most groups read by one query of {@link #warmUp(BooleanSupplier, PutBudget)}. */
    static final int WARM_UP_PAGE_SIZE = 100;

    // The empty id sorts before the ids of the groups
    private static final String BEFORE_FIRST_ID = "";

    /** @param template */
    public PgconfigLayerGroupRepository(@NonNull JdbcTemplate template, @NonNull PgconfigStyleRepository styleLoader) {
        super(LayerGroupInfo.class, template, styleLoader);
    }

    @Override
    protected String getReturnColumns() {
        return CatalogInfoRowMapper.LAYERGROUPINFO_BUILD_COLUMNS;
    }

    /** The lookups of one group also select its members and its GWC settings. */
    @Override
    protected String getLookupColumns() {
        return COLUMNS_WITH_MEMBERS_AND_TILE_LAYER;
    }

    /** The bulk lookups by id also select the GWC settings of the groups, and not their members. */
    @Override
    protected String getBulkColumns() {
        return COLUMNS_WITH_TILE_LAYER;
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

    /**
     * Puts the groups in the entity cache with their members, ahead of their lookups. The access checks look up the
     * groups containing a layer by id, and each lookup missing the cache runs a query of its own.
     *
     * <p>The groups are read in pages ordered by id, with the columns of the lookup of one group, members included.
     * Their rows are put without decoding their objects. The puts of a page take the epoch read before its query: an
     * eviction during the query drops them. The warm-up stops after the last page, when cancelled, once the budget is
     * spent, or when the cache is not in use, as during a reload. Without a cache, it runs nothing.
     *
     * @param cancelled answers true when the caller no longer needs the warm-up, checked before each page
     * @param budget bounds the weight put in the cache, bound to the thread during the warm-up
     * @return the number of groups put
     */
    public int warmUp(@NonNull BooleanSupplier cancelled, @NonNull PutBudget budget) {
        try (PutBudget.Binding binding = budget.bind()) {
            return warmUpPages(cancelled, budget);
        }
    }

    private int warmUpPages(BooleanSupplier cancelled, PutBudget budget) {
        int groupsPut = 0;
        String lastId = BEFORE_FIRST_ID;
        boolean morePages = true;
        while (morePages && canWarmUp(cancelled, budget)) {
            WarmUpPage page = warmUpPageAfter(lastId);
            groupsPut += page.groupsPut();
            lastId = page.lastId();
            morePages = page.isFull();
        }
        return groupsPut;
    }

    // A suspended cache ignores puts
    private boolean canWarmUp(BooleanSupplier cancelled, PutBudget budget) {
        return cacheInUse() && !budget.isSpent() && !cancelled.getAsBoolean();
    }

    private WarmUpPage warmUpPageAfter(String lastId) {
        WarmUpPage page = new WarmUpPage(lastId);
        putRows(warmUpQuery(), page::add, lastId, WARM_UP_PAGE_SIZE);
        return page;
    }

    /**
     * The groups after an id, with their id for the next page and the columns of the lookup of one group, members
     * included.
     */
    private String warmUpQuery() {
        String predicate =
                """
                WHERE "@type" = 'LayerGroupInfo' AND id > ? ORDER BY id LIMIT ?
                """;
        return selectColumns("id, " + getLookupColumns(), predicate);
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

    /** A page of the warm-up: the id of its last row, the rows read and the groups put. */
    private static final class WarmUpPage {

        private String lastId;
        private int rows;
        private int groupsPut;

        WarmUpPage(String afterId) {
            this.lastId = afterId;
        }

        void add(ResultSet rs, boolean stored) throws SQLException {
            lastId = rs.getString("id");
            rows++;
            if (stored) {
                groupsPut++;
            }
        }

        String lastId() {
            return lastId;
        }

        int groupsPut() {
            return groupsPut;
        }

        // A page with fewer rows than the limit is the last one
        boolean isFull() {
            return rows == WARM_UP_PAGE_SIZE;
        }
    }
}
