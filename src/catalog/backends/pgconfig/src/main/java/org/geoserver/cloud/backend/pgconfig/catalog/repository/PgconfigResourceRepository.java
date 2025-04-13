/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.catalog.repository;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @since 1.4
 */
public class PgconfigResourceRepository extends PgconfigCatalogInfoRepository<ResourceInfo>
        implements ResourceRepository {

    private static final String AND_TYPE_INFOTYPE = " AND \"@type\" = ?::infotype";
    private final PgconfigLayerRepository layerrepo;

    /**
     * @param template
     */
    public PgconfigResourceRepository(@NonNull JdbcTemplate template, @NonNull PgconfigLayerRepository layerrepo) {
        super(ResourceInfo.class, template);
        this.layerrepo = layerrepo;
    }

    @Override
    protected String getQueryTable() {
        return "resourceinfos";
    }

    @Override
    protected String getReturnColumns() {
        return CatalogInfoRowMapper.RESOURCEINFO_BUILD_COLUMNS;
    }

    @Override
    public <R extends ResourceInfo> R update(@NonNull R value, @NonNull Patch patch) {
        Optional<ResourceInfo> oldResource = super.findById(value.getId());
        R patched = super.update(value, patch);
        updateLayer(oldResource.orElseThrow(), patched);
        return patched;
    }

    private void updateLayer(ResourceInfo oldResource, ResourceInfo patched) {
        if (!oldResource.getName().equals(patched.getName())) {
            Optional<LayerInfo> layer = layerrepo.findOneByName(oldResource.prefixedName());
            layer.ifPresent(
                    // update the layer's json name which will update the layerinfo.name computed
                    // field
                    li -> {
                        Patch p = PropertyDiff.builder(li)
                                .with("name", patched.getName())
                                .build()
                                .toPatch();
                        layerrepo.update(li, p);
                    });
        }
    }

    @Override
    public <T extends ResourceInfo> Optional<T> findByNameAndNamespace(
            @NonNull String name, @NonNull NamespaceInfo namespace, @NonNull Class<T> clazz) {
        String query = select("""
                WHERE "namespace.id" = ? AND name = ?
                """);

        if (ResourceInfo.class.equals(clazz)) {
            return findOne(query, clazz, namespace.getId(), name);
        }
        query += AND_TYPE_INFOTYPE;
        String infoType = infoType(clazz);
        return findOne(query, clazz, namespace.getId(), name, infoType);
    }

    @Override
    public <T extends ResourceInfo> Stream<T> findAllByType(@NonNull Class<T> clazz) {

        if (ResourceInfo.class.equals(clazz)) {
            return queryForStream(clazz, select(null));
        }
        String query = select("""
                        WHERE "@type" = ?::infotype
                        """);
        String infoType = infoType(clazz);
        return queryForStream(clazz, query, infoType);
    }

    @Override
    public <T extends ResourceInfo> Stream<T> findAllByNamespace(@NonNull NamespaceInfo ns, @NonNull Class<T> clazz) {

        String query = select("""
                WHERE "namespace.id" = ?
                """);
        if (ResourceInfo.class.equals(clazz)) {
            return queryForStream(clazz, query, ns.getId());
        }
        query += AND_TYPE_INFOTYPE;
        return queryForStream(clazz, query, ns.getId(), infoType(clazz));
    }

    @Override
    public <T extends ResourceInfo> Optional<T> findByStoreAndName(
            @NonNull StoreInfo store, @NonNull String name, @NonNull Class<T> clazz) {

        String query = select("""
                WHERE "store.id" = ? AND name = ?
                """);
        if (ResourceInfo.class.equals(clazz)) {
            return findOne(query, clazz, store.getId(), name);
        }
        query += AND_TYPE_INFOTYPE;
        return findOne(query, clazz, store.getId(), name, infoType(clazz));
    }

    @Override
    public <T extends ResourceInfo> Stream<T> findAllByStore(StoreInfo store, Class<T> clazz) {
        String query = select("""
                WHERE "store.id" = ?
                """);
        if (ResourceInfo.class.equals(clazz)) {
            return queryForStream(clazz, query, store.getId());
        }
        query += AND_TYPE_INFOTYPE;
        return super.queryForStream(clazz, query, store.getId(), infoType(clazz));
    }
}
