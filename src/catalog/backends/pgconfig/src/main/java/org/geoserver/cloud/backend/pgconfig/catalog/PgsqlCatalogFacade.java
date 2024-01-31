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
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgsqlCatalogInfoRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgsqlLayerGroupRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgsqlLayerRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgsqlNamespaceRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgsqlResourceRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgsqlStoreRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgsqlStyleRepository;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgsqlWorkspaceRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * @since 1.4
 */
public class PgsqlCatalogFacade extends RepositoryCatalogFacadeImpl {

    public PgsqlCatalogFacade(@NonNull JdbcTemplate template) {
        PgsqlNamespaceRepository namespaces = new PgsqlNamespaceRepository(template);
        PgsqlWorkspaceRepository workspaces = new PgsqlWorkspaceRepository(template);
        PgsqlStyleRepository styles = new PgsqlStyleRepository(template);

        PgsqlStoreRepository stores = new PgsqlStoreRepository(template);
        PgsqlLayerRepository layers = new PgsqlLayerRepository(template, styles);
        PgsqlResourceRepository resources = new PgsqlResourceRepository(template, layers);
        PgsqlLayerGroupRepository layerGroups = new PgsqlLayerGroupRepository(template);

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
                .map(PgsqlCatalogInfoRepository.class::cast)
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
