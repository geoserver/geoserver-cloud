/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import org.geoserver.cloud.catalog.client.feign.LayerClient;
import org.geoserver.cloud.catalog.client.feign.LayerGroupClient;
import org.geoserver.cloud.catalog.client.feign.MapClient;
import org.geoserver.cloud.catalog.client.feign.NamespaceClient;
import org.geoserver.cloud.catalog.client.feign.ResourceClient;
import org.geoserver.cloud.catalog.client.feign.StoreClient;
import org.geoserver.cloud.catalog.client.feign.StyleClient;
import org.geoserver.cloud.catalog.client.feign.WorkspaceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogRepositoriesConfiguration {

    private @Autowired WorkspaceClient workspaceClient;
    private @Autowired NamespaceClient namespaceClient;
    private @Autowired StoreClient storeClient;
    private @Autowired ResourceClient resourceClient;
    private @Autowired LayerClient layerClient;
    private @Autowired LayerGroupClient layerGroupClient;
    private @Autowired StyleClient styleClient;
    private @Autowired MapClient mapClient;

    public @Bean CloudWorkspaceRepository cloudWorkspaceRepository() {
        return new CloudWorkspaceRepository(workspaceClient);
    }

    public @Bean CloudNamespaceRepository cloudNamespaceRepository() {
        return new CloudNamespaceRepository(namespaceClient);
    }

    public @Bean CloudStoreRepository cloudStoreRepository() {
        return new CloudStoreRepository(storeClient);
    }

    public @Bean CloudResourceRepository cloudResourceRepository() {
        return new CloudResourceRepository(resourceClient);
    }

    public @Bean CloudLayerRepository cloudLayerRepository() {
        return new CloudLayerRepository(layerClient);
    }

    public @Bean CloudLayerGroupRepository cloudLayerGroupRepository() {
        return new CloudLayerGroupRepository(layerGroupClient);
    }

    public @Bean CloudStyleRepository cloudStyleRepository() {
        return new CloudStyleRepository(styleClient);
    }

    public @Bean CloudMapRepository cloudMapRepository() {
        return new CloudMapRepository(mapClient);
    }
}
