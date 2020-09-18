/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveCatalogClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogRepositoriesConfiguration {

    private @Autowired ReactiveCatalogClient client;

    public @Bean CloudWorkspaceRepository cloudWorkspaceRepository() {
        return new CloudWorkspaceRepository(client);
    }

    public @Bean CloudNamespaceRepository cloudNamespaceRepository() {
        return new CloudNamespaceRepository(client);
    }

    public @Bean CloudStoreRepository cloudStoreRepository() {
        return new CloudStoreRepository(client);
    }

    public @Bean CloudResourceRepository cloudResourceRepository() {
        return new CloudResourceRepository(client);
    }

    public @Bean CloudLayerRepository cloudLayerRepository() {
        return new CloudLayerRepository(client);
    }

    public @Bean CloudLayerGroupRepository cloudLayerGroupRepository() {
        return new CloudLayerGroupRepository(client);
    }

    public @Bean CloudStyleRepository cloudStyleRepository() {
        return new CloudStyleRepository(client);
    }

    public @Bean CloudMapRepository cloudMapRepository() {
        return new CloudMapRepository(client);
    }
}
