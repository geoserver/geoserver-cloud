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
import org.geoserver.cloud.catalog.client.repository.CatalogClientLayerGroupRepository;
import org.geoserver.cloud.catalog.client.repository.CatalogClientLayerRepository;
import org.geoserver.cloud.catalog.client.repository.CatalogClientMapRepository;
import org.geoserver.cloud.catalog.client.repository.CatalogClientNamespaceRepository;
import org.geoserver.cloud.catalog.client.repository.CatalogClientRepositoryConfiguration;
import org.geoserver.cloud.catalog.client.repository.CatalogClientResourceRepository;
import org.geoserver.cloud.catalog.client.repository.CatalogClientStoreRepository;
import org.geoserver.cloud.catalog.client.repository.CatalogClientStyleRepository;
import org.geoserver.cloud.catalog.client.repository.CatalogClientWorkspaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CatalogClientRepositoryConfiguration.class)
public class CatalogClientConfiguration {

    private @Autowired CatalogClientWorkspaceRepository cloudWorkspaceRepository;
    private @Autowired CatalogClientNamespaceRepository cloudNamespaceRepository;
    private @Autowired CatalogClientStoreRepository cloudStoreRepository;
    private @Autowired CatalogClientResourceRepository cloudResourceRepository;
    private @Autowired CatalogClientLayerRepository cloudLayerRepository;
    private @Autowired CatalogClientLayerGroupRepository cloudLayerGroupRepository;
    private @Autowired CatalogClientStyleRepository cloudStyleRepository;
    private @Autowired CatalogClientMapRepository cloudMapRepository;

    private @Autowired ReactiveConfigClient configClient;
    private @Autowired ReactiveResourceStoreClient resourceStoreClient;

    @Bean
    CatalogClientCatalogFacade rawCatalogServiceFacade() {
        RepositoryCatalogFacade rawFacade = new RepositoryCatalogFacadeImpl();
        rawFacade.setWorkspaceRepository(cloudWorkspaceRepository);
        rawFacade.setNamespaceRepository(cloudNamespaceRepository);
        rawFacade.setStoreRepository(cloudStoreRepository);
        rawFacade.setResourceRepository(cloudResourceRepository);
        rawFacade.setLayerRepository(cloudLayerRepository);
        rawFacade.setLayerGroupRepository(cloudLayerGroupRepository);
        rawFacade.setStyleRepository(cloudStyleRepository);
        rawFacade.setMapRepository(cloudMapRepository);

        CatalogClientCatalogFacade facade = new CatalogClientCatalogFacade(rawFacade);
        return facade;
    }

    @Bean
    CatalogClientConfigRepository catalogServiceConfigRepository() {
        return new CatalogClientConfigRepository(configClient);
    }

    @Bean
    CatalogClientGeoServerFacade catalogServiceGeoServerFacade() {
        return new CatalogClientGeoServerFacade(catalogServiceConfigRepository());
    }

    @Bean
    CatalogClientResourceStore catalogServiceResourceStore() {
        BlockingResourceStoreClient blockingClient =
                new BlockingResourceStoreClient(resourceStoreClient);
        return new CatalogClientResourceStore(blockingClient);
    }

    // @ConditionalOnProperty(name = "reactive.feign.jetty", havingValue = "true")
    // @Bean JettyHttpClientFactory jettyHttpClientFactory() {
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
