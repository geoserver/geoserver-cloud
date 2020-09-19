/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.impl;

import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerGroupRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.LayerRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.MapRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.NamespaceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.ResourceRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StoreRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.StyleRepository;
import org.geoserver.catalog.plugin.CatalogInfoRepository.WorkspaceRepository;
import org.geoserver.cloud.catalog.client.repository.CatalogRepositoriesConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({CatalogRepositoriesConfiguration.class})
public class CatalogClientConfiguration {

    private @Autowired WorkspaceRepository cloudWorkspaceRepository;
    private @Autowired NamespaceRepository cloudNamespaceRepository;
    private @Autowired StoreRepository cloudStoreRepository;
    private @Autowired ResourceRepository cloudResourceRepository;
    private @Autowired LayerRepository cloudLayerRepository;
    private @Autowired LayerGroupRepository cloudLayerGroupRepository;
    private @Autowired StyleRepository cloudStyleRepository;
    private @Autowired MapRepository cloudMapRepository;

    public @Bean CloudCatalogFacade cloudCatalogFacade() {
        CloudCatalogFacade facade = new CloudCatalogFacade();
        facade.setWorkspaces(cloudWorkspaceRepository);
        facade.setNamespaces(cloudNamespaceRepository);
        facade.setStores(cloudStoreRepository);
        facade.setResources(cloudResourceRepository);
        facade.setLayers(cloudLayerRepository);
        facade.setLayerGroups(cloudLayerGroupRepository);
        facade.setStyles(cloudStyleRepository);
        facade.setMaps(cloudMapRepository);
        return facade;
    }
}
