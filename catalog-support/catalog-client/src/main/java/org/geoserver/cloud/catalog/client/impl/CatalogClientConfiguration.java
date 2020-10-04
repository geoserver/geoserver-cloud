/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.impl;

import org.geoserver.catalog.plugin.RepositoryCatalogFacade;
import org.geoserver.catalog.plugin.RepositoryCatalogFacadeImpl;
import org.geoserver.cloud.catalog.client.reactivefeign.BlockingResourceStoreClient;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveConfigClient;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveResourceStoreClient;
import org.geoserver.cloud.catalog.client.repository.CatalogClientConfigRepository;
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

    private @Autowired ReactiveConfigClient configClient;
    private @Autowired ReactiveResourceStoreClient resourceStoreClient;

    public @Bean CatalogServiceCatalogFacade rawCatalogServiceFacade() {
        RepositoryCatalogFacade rawFacade = new RepositoryCatalogFacadeImpl();
        rawFacade.setWorkspaceRepository(cloudWorkspaceRepository);
        rawFacade.setNamespaceRepository(cloudNamespaceRepository);
        rawFacade.setStoreRepository(cloudStoreRepository);
        rawFacade.setResourceRepository(cloudResourceRepository);
        rawFacade.setLayerRepository(cloudLayerRepository);
        rawFacade.setLayerGroupRepository(cloudLayerGroupRepository);
        rawFacade.setStyleRepository(cloudStyleRepository);
        rawFacade.setMapRepository(cloudMapRepository);
        
        CatalogServiceCatalogFacade facade = new CatalogServiceCatalogFacade(rawFacade);
        return facade;
    }

    public @Bean CatalogClientConfigRepository catalogServiceConfigRepository() {
        return new CatalogClientConfigRepository(configClient);
    }

    public @Bean CatalogServiceGeoServerFacade catalogServiceGeoServerFacade() {
        return new CatalogServiceGeoServerFacade(catalogServiceConfigRepository());
    }

    public @Bean CatalogServiceResourceStore catalogServiceResourceStore() {
        BlockingResourceStoreClient blockingClient =
                new BlockingResourceStoreClient(resourceStoreClient);
        return new CatalogServiceResourceStore(blockingClient);
    }

    // @ConditionalOnProperty(name = "reactive.feign.jetty", havingValue = "true")
    // public @Bean JettyHttpClientFactory jettyHttpClientFactory() {
    // return new JettyHttpClientFactory() {
    //
    // @Override
    // public HttpClient build(boolean useHttp2) {
    // HttpClient httpClient = new HttpClient();
    // try {
    // httpClient.start();
    // } catch (Exception e) {
    // e.printStackTrace();
    // throw new RuntimeException(e);
    // }
    // return httpClient;
    // }
    // };
    // }
}
