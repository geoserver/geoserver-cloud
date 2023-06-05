/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.catalog.repository;

import lombok.Getter;
import lombok.NonNull;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @since 1.4
 */
public class PgsqlLayerRepository extends PgsqlCatalogInfoRepository<LayerInfo>
        implements LayerRepository {

    private final @Getter Class<LayerInfo> contentType = LayerInfo.class;
    private final @Getter String queryTable = "layerinfos";

    /**
     * @param template
     */
    public PgsqlLayerRepository(@NonNull JdbcTemplate template) {
        super(template);
    }

    @Override
    public Optional<LayerInfo> findOneByName(@NonNull String possiblyPrefixedName) {
        String sql =
                """
                SELECT publishedinfo, resource, store, workspace, namespace, "defaultStyle"
                FROM layerinfos
                WHERE "%s" = ?
                """;
        if (possiblyPrefixedName.contains(":")) {
            // two options here, it's either a prefixed name like in <workspace>:<name>, or the
            // ResourceInfo name actually contains a colon
            Optional<LayerInfo> found =
                    findOne(
                            sql.formatted("prefixedName"),
                            LayerInfo.class,
                            newRowMapper(),
                            possiblyPrefixedName);
            if (found.isPresent()) return found;
        }

        // no colon in name or name actually contains a colon
        return findOne(
                sql.formatted("name"), LayerInfo.class, newRowMapper(), possiblyPrefixedName);
    }

    // TODO: optimize
    @Override
    public Stream<LayerInfo> findAllByDefaultStyleOrStyles(@NonNull StyleInfo style) {
        return findAll().filter(styleFilter(style));
    }

    private Predicate<LayerInfo> styleFilter(@NonNull StyleInfo style) {
        return l -> {
            if (matches(style, l.getDefaultStyle())) return true;
            return Optional.ofNullable(l.getStyles()).orElse(Set.of()).stream()
                    .anyMatch(s -> matches(style, s));
        };
    }

    private boolean matches(StyleInfo expected, StyleInfo actual) {
        return actual != null && expected.getId().equals(actual.getId());
    }

    @Override
    public Stream<LayerInfo> findAllByResource(@NonNull ResourceInfo resource) {
        String sql =
                """
                SELECT publishedinfo, resource, store, workspace, namespace, "defaultStyle"
                FROM layerinfos
                WHERE "resource.id" = ?
                """;
        return template.queryForStream(sql, newRowMapper(), resource.getId());
    }

    @Override
    protected RowMapper<LayerInfo> newRowMapper() {
        PgsqlStyleRepository styleLoader = new PgsqlStyleRepository(template);
        return CatalogInfoRowMapper.layer(styleLoader::findById);
    }
}
