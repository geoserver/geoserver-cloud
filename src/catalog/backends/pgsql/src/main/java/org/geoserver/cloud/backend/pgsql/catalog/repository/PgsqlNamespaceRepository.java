/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgsql.catalog.repository;

import lombok.NonNull;

import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * @since 1.4
 */
public class PgsqlNamespaceRepository extends PgsqlCatalogInfoRepository<NamespaceInfo>
        implements NamespaceRepository {

    /**
     * @param template
     */
    public PgsqlNamespaceRepository(@NonNull JdbcTemplate template) {
        super(template);
    }

    @Override
    public Class<NamespaceInfo> getContentType() {
        return NamespaceInfo.class;
    }

    @Override
    protected String getQueryTable() {
        return "namespaceinfos";
    }

    @Override
    public void setDefaultNamespace(@NonNull NamespaceInfo namespace) {
        unsetDefaultNamespace();
        template.update(
                """
                UPDATE namespaceinfo SET default_namespace = TRUE WHERE id = ?
                """,
                namespace.getId());
    }

    @Override
    public void unsetDefaultNamespace() {
        template.update(
                """
                UPDATE namespaceinfo SET default_namespace = FALSE WHERE default_namespace = TRUE
                """);
    }

    @Override
    public Optional<NamespaceInfo> getDefaultNamespace() {
        return findOne(
                """
                SELECT namespace FROM namespaceinfos WHERE default_namespace = TRUE
                """);
    }

    @Override
    public Optional<NamespaceInfo> findOneByURI(@NonNull String uri) {
        return findOne(
                """
                SELECT namespace FROM namespaceinfos WHERE uri = ?
                """,
                uri);
    }

    @Override
    public Stream<NamespaceInfo> findAllByURI(@NonNull String uri) {
        return template.queryForStream(
                """
                SELECT namespace FROM namespaceinfos WHERE uri = ?
                """,
                newRowMapper(),
                uri);
    }

    @Override
    protected RowMapper<NamespaceInfo> newRowMapper() {
        return CatalogInfoRowMapper.namespace();
    }
}
