/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import lombok.NonNull;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.catalog.plugin.Query;
import org.geotools.api.filter.Filter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * @since 1.4
 */
public class PgconfigLayerRepository extends PgconfigCatalogInfoRepository<LayerInfo>
        implements LayerRepository {

    private final PgconfigStyleRepository styleLoader;

    /**
     * @param template
     */
    public PgconfigLayerRepository(
            @NonNull JdbcTemplate template, @NonNull PgconfigStyleRepository styleLoader) {
        super(template);
        this.styleLoader = styleLoader;
    }

    @Override
    public Class<LayerInfo> getContentType() {
        return LayerInfo.class;
    }

    @Override
    protected String getQueryTable() {
        return "layerinfos";
    }

    @Override
    public Optional<LayerInfo> findOneByName(@NonNull String possiblyPrefixedName) {
        String sql =
                """
                SELECT publishedinfo, resource, store, workspace, namespace, "defaultStyle" \
                FROM layerinfos \
                WHERE "%s" = ?
                """;
        if (possiblyPrefixedName.contains(":")) {
            // two options here, it's either a prefixed name like in <workspace>:<name>, or the
            // ResourceInfo name actually contains a colon
            Optional<LayerInfo> found =
                    findOne(sql.formatted("prefixedName"), possiblyPrefixedName);
            if (found.isPresent()) return found;
        }

        // no colon in name or name actually contains a colon
        return findOne(sql.formatted("name"), possiblyPrefixedName);
    }

    @Override
    public Stream<LayerInfo> findAllByDefaultStyleOrStyles(@NonNull StyleInfo style) {
        var ff = FILTER_FACTORY;
        Filter filter =
                ff.or(
                        ff.equals(ff.property("defaultStyle.id"), ff.literal(style.getId())),
                        ff.equals(ff.property("styles.id"), ff.literal(style.getId())));

        return findAll(Query.valueOf(LayerInfo.class, filter));
    }

    @Override
    public Stream<LayerInfo> findAllByResource(@NonNull ResourceInfo resource) {
        String sql =
                """
                SELECT publishedinfo, resource, store, workspace, namespace, "defaultStyle" \
                FROM layerinfos \
                WHERE "resource.id" = ?
                """;
        return super.queryForStream(sql, resource.getId());
    }

    @Override
    protected RowMapper<LayerInfo> newRowMapper() {
        return CatalogInfoRowMapper.layer(styleLoader::findById);
    }
}
