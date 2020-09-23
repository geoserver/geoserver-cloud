/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.impl;

import org.geoserver.cloud.catalog.client.repository.CatalogRepositoriesConfiguration;
import org.geoserver.cloud.catalog.client.repository.CloudLayerGroupRepository;
import org.geoserver.cloud.catalog.client.repository.CloudLayerRepository;
import org.geoserver.cloud.catalog.client.repository.CloudMapRepository;
import org.geoserver.cloud.catalog.client.repository.CloudNamespaceRepository;
import org.geoserver.cloud.catalog.client.repository.CloudResourceRepository;
import org.geoserver.cloud.catalog.client.repository.CloudStoreRepository;
import org.geoserver.cloud.catalog.client.repository.CloudStyleRepository;
import org.geoserver.cloud.catalog.client.repository.CloudWorkspaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CatalogRepositoriesConfiguration.class)
public class CatalogClientConfiguration {

    private @Autowired CloudWorkspaceRepository cloudWorkspaceRepository;
    private @Autowired CloudNamespaceRepository cloudNamespaceRepository;
    private @Autowired CloudStoreRepository cloudStoreRepository;
    private @Autowired CloudResourceRepository cloudResourceRepository;
    private @Autowired CloudLayerRepository cloudLayerRepository;
    private @Autowired CloudLayerGroupRepository cloudLayerGroupRepository;
    private @Autowired CloudStyleRepository cloudStyleRepository;
    private @Autowired CloudMapRepository cloudMapRepository;

    public @Bean CatalogServiceCatalogFacade rawCatalogServiceFacade() {
        CatalogServiceCatalogFacade facade = new CatalogServiceCatalogFacade();
        facade.setWorkspaces(cloudWorkspaceRepository);
        facade.setNamespaces(cloudNamespaceRepository);
        facade.setStores(cloudStoreRepository);
        facade.setResources(cloudResourceRepository);
        facade.setLayers(cloudLayerRepository);
        facade.setLayerGroups(cloudLayerGroupRepository);
        facade.setStyles(cloudStyleRepository);
        facade.setMaps(cloudMapRepository);
        InnerResolvingProxy resolver = new InnerResolvingProxy(facade, null);
        facade.setObjectResolver(resolver::resolve);
        return facade;
    }
}
