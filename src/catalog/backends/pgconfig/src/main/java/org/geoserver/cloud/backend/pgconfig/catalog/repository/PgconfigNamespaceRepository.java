/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.CatalogInfoKeys;
import org.springframework.jdbc.core.JdbcTemplate;

/** @since 1.4 */
public class PgconfigNamespaceRepository extends PgconfigCatalogInfoRepository<NamespaceInfo>
        implements NamespaceRepository {

    /** @param template */
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

    /** The prefix of a namespace is unique, and is the key of its {@code NAME} entry. */
    @Override
    public <U extends NamespaceInfo> Optional<U> findFirstByName(@NonNull String prefix, Class<U> clazz) {
        List<CacheKey> keys = nameKeys(prefix, clazz);
        return findByKeys(keys, clazz, () -> super.findFirstByName(prefix, clazz));
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
        evictAfterWrite(CatalogInfoKeys.defaultNamespace());
    }

    @Override
    public void unsetDefaultNamespace() {
        template.update(
                """
                UPDATE %s SET default_namespace = FALSE WHERE default_namespace = TRUE
                """
                        .formatted(getUpdateTable()));
        evictAfterWrite(CatalogInfoKeys.defaultNamespace());
    }

    @Override
    public Optional<NamespaceInfo> getDefaultNamespace() {
        List<CacheKey> keys = List.of(CatalogInfoKeys.defaultNamespace());
        return findByKeys(keys, NamespaceInfo.class, () -> findOne(select("WHERE default_namespace = TRUE")));
    }

    /** Several namespaces may share a URI, as isolated workspaces do: the key holds the first one, by id. */
    @Override
    public Optional<NamespaceInfo> findOneByURI(@NonNull String uri) {
        List<CacheKey> keys = List.of(CatalogInfoKeys.namespaceByUri(uri));
        return findByKeys(keys, NamespaceInfo.class, () -> findOne(select("WHERE uri = ? ORDER BY id"), uri));
    }

    @Override
    public Stream<NamespaceInfo> findAllByURI(@NonNull String uri) {
        return queryForStream(select("WHERE uri = ?"), uri);
    }
}
