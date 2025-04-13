/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.catalog.plugin.Query;
import org.geotools.api.filter.Filter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * @since 1.4
 */
public class PgconfigLayerRepository extends PgconfigPublishedInfoRepository<LayerInfo> implements LayerRepository {

    /**
     * @param template
     */
    public PgconfigLayerRepository(@NonNull JdbcTemplate template, @NonNull PgconfigStyleRepository styleLoader) {
        super(LayerInfo.class, template, styleLoader);
    }

    @Override
    protected String getReturnColumns() {
        return CatalogInfoRowMapper.LAYERINFO_BUILD_COLUMNS;
    }

    @Override
    protected final RowMapper<LayerInfo> newRowMapper() {
        return CatalogInfoRowMapper.<LayerInfo>newInstance().setStyleLoader(styleRepo::findById);
    }

    @Override
    public Optional<LayerInfo> findOneByName(@NonNull String possiblyPrefixedName) {
        String sql =
                """
                SELECT "@type", publishedinfo, resource, store, workspace, namespace, "defaultStyle" \
                FROM %s \
                WHERE "@type" = 'LayerInfo' AND "%s" = ?
                """;
        if (possiblyPrefixedName.contains(":")) {
            // two options here, it's either a prefixed name like in <workspace>:<name>, or the
            // ResourceInfo name actually contains a colon
            Optional<LayerInfo> found = findOne(sql.formatted(getQueryTable(), "prefixedName"), possiblyPrefixedName);
            if (found.isPresent()) {
                return found;
            }
        }

        // no colon in name or name actually contains a colon
        return findOne(sql.formatted(getQueryTable(), "name"), possiblyPrefixedName);
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
                SELECT "@type", publishedinfo, resource, store, workspace, namespace, "defaultStyle" \
                FROM %s \
                WHERE "@type" = 'LayerInfo' AND "resource.id" = ?
                """
                        .formatted(getQueryTable());
        return super.queryForStream(sql, resource.getId());
    }
}
