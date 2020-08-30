/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.repository.caching;

import javax.annotation.PostConstruct;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.plugin.AbstractCatalogFacade;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.MapRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables caching at the {@link CatalogInfoRepository} level instead of at the {@link Catalog}
 * level, which would be the natural choice, in order not to interfere with catalog decorators such
 * as {@code SecureCatalogImpl}, which need to hide objects at runtime, and if a caching decorator
 * sits on top of it, those resources might not be hidden for a given user when they should.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(
    prefix = "geoserver.catalog.caching",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class CachingRepositoryConfiguration {

    private @Autowired(required = false) WorkspaceRepository workspaces;
    private @Autowired(required = false) NamespaceRepository namespaces;
    private @Autowired(required = false) StoreRepository stores;
    private @Autowired(required = false) ResourceRepository resources;
    private @Autowired(required = false) LayerRepository layers;
    private @Autowired(required = false) LayerGroupRepository layerGroups;
    private @Autowired(required = false) StyleRepository styles;
    private @Autowired(required = false) MapRepository maps;

    private @Autowired @Qualifier("rawCatalog") Catalog rawCatalog;

    public @PostConstruct void decorateCatalogFacade() {
        CatalogFacade facade = rawCatalog.getFacade();

        if (!(facade instanceof AbstractCatalogFacade)) {
            throw new IllegalStateException();
        }
        AbstractCatalogFacade fc = (AbstractCatalogFacade) facade;
        if (workspaces != null) fc.setWorkspaces(cachingWorkspaceRepository());
        if (namespaces != null) fc.setNamespaces(cachingNamespaceRepository());
        if (stores != null) fc.setStores(cachingStoreRepository());
        if (resources != null) fc.setResources(cachingResourceRepository());
        if (layers != null) fc.setLayers(cachingLayerRepository());
        if (layerGroups != null) fc.setLayerGroups(cachingLayerGroupRepository());
        if (styles != null) fc.setStyles(cachingStyleRepository());
        if (maps != null) fc.setMaps(cachingMapRepository());
    }

    @ConditionalOnBean(value = WorkspaceRepository.class)
    public @Bean CachingWorkspaceRepository cachingWorkspaceRepository() {
        return new CachingWorkspaceRepository(workspaces);
    }

    @ConditionalOnBean(value = NamespaceRepository.class)
    public @Bean CachingNamespaceRepository cachingNamespaceRepository() {
        return new CachingNamespaceRepository(namespaces);
    }

    @ConditionalOnBean(value = StoreRepository.class)
    public @Bean CachingStoreRepository cachingStoreRepository() {
        return new CachingStoreRepository(stores);
    }

    @ConditionalOnBean(value = ResourceRepository.class)
    public @Bean CachingResourceRepository cachingResourceRepository() {
        return new CachingResourceRepository(resources);
    }

    @ConditionalOnBean(value = LayerRepository.class)
    public @Bean CachingLayerRepository cachingLayerRepository() {
        return new CachingLayerRepository(layers);
    }

    @ConditionalOnBean(value = LayerGroupRepository.class)
    public @Bean CachingLayerGroupRepository cachingLayerGroupRepository() {
        return new CachingLayerGroupRepository(layerGroups);
    }

    @ConditionalOnBean(value = StyleRepository.class)
    public @Bean CachingStyleRepository cachingStyleRepository() {
        return new CachingStyleRepository(styles);
    }

    @ConditionalOnBean(value = MapRepository.class)
    public @Bean CachingMapRepository cachingMapRepository() {
        return new CachingMapRepository(maps);
    }
}
