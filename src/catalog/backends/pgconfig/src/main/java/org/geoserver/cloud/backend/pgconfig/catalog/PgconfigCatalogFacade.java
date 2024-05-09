/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog;

import lombok.NonNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.plugin.RepositoryCatalogFacadeImpl;
import org.geoserver.catalog.plugin.resolving.CatalogPropertyResolver;
import org.geoserver.catalog.plugin.resolving.CollectionPropertiesInitializer;
import org.geoserver.catalog.plugin.resolving.ResolvingProxyResolver;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigCatalogInfoRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigLayerGroupRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigLayerRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigNamespaceRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigResourceRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigStoreRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigStyleRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigWorkspaceRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * @since 1.4
 */
public class PgconfigCatalogFacade extends RepositoryCatalogFacadeImpl {

    public PgconfigCatalogFacade(@NonNull JdbcTemplate template) {
        PgconfigNamespaceRepository namespaces = new PgconfigNamespaceRepository(template);
        PgconfigWorkspaceRepository workspaces = new PgconfigWorkspaceRepository(template);
        PgconfigStyleRepository styles = new PgconfigStyleRepository(template);

        PgconfigStoreRepository stores = new PgconfigStoreRepository(template);
        PgconfigLayerRepository layers = new PgconfigLayerRepository(template, styles);
        PgconfigResourceRepository resources = new PgconfigResourceRepository(template, layers);
        PgconfigLayerGroupRepository layerGroups = new PgconfigLayerGroupRepository(template);

        super.setNamespaceRepository(namespaces);
        super.setWorkspaceRepository(workspaces);
        super.setStoreRepository(stores);
        super.setResourceRepository(resources);
        super.setLayerRepository(layers);
        super.setLayerGroupRepository(layerGroups);
        super.setStyleRepository(styles);
    }

    @Override
    public void setCatalog(Catalog catalog) {
        super.setCatalog(catalog);
        setOutboundResolver();
    }

    @SuppressWarnings("unchecked")
    private void setOutboundResolver() {
        UnaryOperator<CatalogInfo> resolvingFunction = resolvingFunction(this::getCatalog);

        super.repositories.all().stream()
                .map(PgconfigCatalogInfoRepository.class::cast)
                .forEach(repo -> repo.setOutboundResolver(resolvingFunction));
    }

    public static <T extends CatalogInfo> UnaryOperator<T> resolvingFunction(
            Supplier<Catalog> catalog) {
        return CatalogPropertyResolver.<T>of(catalog)
                        .andThen(ResolvingProxyResolver.<T>of(catalog))
                        .andThen(CollectionPropertiesInitializer.instance())
                ::apply;
    }
}
