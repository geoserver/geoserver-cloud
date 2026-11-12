/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.CatalogInfoKeys;
import org.geotools.api.filter.Filter;
import org.springframework.jdbc.core.JdbcTemplate;

/** @since 1.4 */
public class PgconfigLayerRepository extends PgconfigPublishedInfoRepository<LayerInfo> implements LayerRepository {

    /** @param template */
    public PgconfigLayerRepository(@NonNull JdbcTemplate template, @NonNull PgconfigStyleRepository styleLoader) {
        super(LayerInfo.class, template, styleLoader);
    }

    @Override
    protected String getReturnColumns() {
        return CatalogInfoRowMapper.LAYERINFO_BUILD_COLUMNS;
    }

    @Override
    protected final CatalogInfoRowMapper<LayerInfo> newRowMapper() {
        return CatalogInfoRowMapper.<LayerInfo>newInstance().setStyleLoader(styleRepo::findById);
    }

    /**
     * Finds a layer by its prefixed name, or by its local name. With a cache, the name is a {@code NAME} key: it holds
     * the id of the layer with that prefixed name, or a miss when no layer has the name, prefixed or local.
     */
    @Override
    public Optional<LayerInfo> findOneByName(@NonNull String possiblyPrefixedName) {
        List<CacheKey> keys = nameKeys(possiblyPrefixedName, LayerInfo.class);
        return findByKeys(keys, LayerInfo.class, () -> findByPrefixedOrLocalName(possiblyPrefixedName));
    }

    /**
     * A layer has the name of its resource. The resource is read from the cache by id, with a decode and no query: the
     * row of the layer put it there with the layer. The {@code NAME} entry of the resource would check nothing: the
     * same row writes it with the {@code NAME} entry of the layer, and both go stale together. A resource absent from
     * the cache cannot confirm the name, and the lookup queries the database.
     */
    @Override
    protected boolean hasName(LayerInfo layer, String prefixedName) {
        ResourceInfo resource = layer.getResource();
        if (null == resource) {
            return false;
        }
        Optional<ResourceInfo> cached = findCached(resource.getId(), ResourceInfo.class);
        return cached.isPresent() && CatalogInfoKeys.hasName(cached.get(), prefixedName);
    }

    private Optional<LayerInfo> findByPrefixedOrLocalName(String possiblyPrefixedName) {
        String sql =
                """
                SELECT %s \
                FROM %s \
                WHERE "@type" = 'LayerInfo' AND "%s" = ? ORDER BY id
                """;
        if (possiblyPrefixedName.contains(":")) {
            // two options here, it's either a prefixed name like in <workspace>:<name>, or the
            // ResourceInfo name actually contains a colon
            Optional<LayerInfo> found =
                    findOne(sql.formatted(getReturnColumns(), getQueryTable(), "prefixedName"), possiblyPrefixedName);
            if (found.isPresent()) {
                return found;
            }
        }

        // no colon in name or name actually contains a colon
        return findOne(sql.formatted(getReturnColumns(), getQueryTable(), "name"), possiblyPrefixedName);
    }

    @Override
    public Stream<LayerInfo> findAllByDefaultStyleOrStyles(@NonNull StyleInfo style) {
        Filter typeFilter = Predicates.isInstanceOf(LayerInfo.class);
        Filter styleFilter = Predicates.or(
                Predicates.equal("defaultStyle.id", style.getId()), Predicates.equal("styles.id", style.getId()));

        Filter filter = Predicates.and(typeFilter, styleFilter);
        return findAll(Query.valueOf(LayerInfo.class, filter));
    }

    @Override
    public Stream<LayerInfo> findAllByResource(@NonNull ResourceInfo resource) {
        String sql =
                """
                SELECT %s \
                FROM %s \
                WHERE "@type" = 'LayerInfo' AND "resource.id" = ?
                """
                        .formatted(getReturnColumns(), getQueryTable());
        return super.queryForStream(sql, resource.getId());
    }
}
