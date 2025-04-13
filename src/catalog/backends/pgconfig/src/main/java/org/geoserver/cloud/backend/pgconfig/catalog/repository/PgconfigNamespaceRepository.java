/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @since 1.4
 */
public class PgconfigNamespaceRepository extends PgconfigCatalogInfoRepository<NamespaceInfo>
        implements NamespaceRepository {

    /**
     * @param template
     */
    public PgconfigNamespaceRepository(@NonNull JdbcTemplate template) {
        super(NamespaceInfo.class, template);
    }

    @Override
    protected String getQueryTable() {
        return "namespaceinfos";
    }

    @Override
    protected String getReturnColumns() {
        return CatalogInfoRowMapper.NAMESPACE_BUILD_COLUMNS;
    }

    @Override
    public void setDefaultNamespace(@NonNull NamespaceInfo namespace) {
        unsetDefaultNamespace();
        template.update(
                """
                UPDATE %s SET default_namespace = TRUE WHERE id = ?
                """
                        .formatted(getUpdateTable()),
                namespace.getId());
    }

    @Override
    public void unsetDefaultNamespace() {
        template.update(
                """
                UPDATE %s SET default_namespace = FALSE WHERE default_namespace = TRUE
                """
                        .formatted(getUpdateTable()));
    }

    @Override
    public Optional<NamespaceInfo> getDefaultNamespace() {
        return findOne(select("WHERE default_namespace = TRUE"));
    }

    @Override
    public Optional<NamespaceInfo> findOneByURI(@NonNull String uri) {
        return findOne(select("WHERE uri = ?"), uri);
    }

    @Override
    public Stream<NamespaceInfo> findAllByURI(@NonNull String uri) {
        return queryForStream(select("WHERE uri = ?"), uri);
    }
}
