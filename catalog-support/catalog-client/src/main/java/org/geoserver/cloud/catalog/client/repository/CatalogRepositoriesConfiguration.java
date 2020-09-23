/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveCatalogApiClientConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(ReactiveCatalogApiClientConfiguration.class)
public class CatalogRepositoriesConfiguration {

    public @Bean CloudWorkspaceRepository cloudWorkspaceRepository() {
        return new CloudWorkspaceRepository();
    }

    public @Bean CloudNamespaceRepository cloudNamespaceRepository() {
        return new CloudNamespaceRepository();
    }

    public @Bean CloudStoreRepository cloudStoreRepository() {
        return new CloudStoreRepository();
    }

    public @Bean CloudResourceRepository cloudResourceRepository() {
        return new CloudResourceRepository();
    }

    public @Bean CloudLayerRepository cloudLayerRepository() {
        return new CloudLayerRepository();
    }

    public @Bean CloudLayerGroupRepository cloudLayerGroupRepository() {
        return new CloudLayerGroupRepository();
    }

    public @Bean CloudStyleRepository cloudStyleRepository() {
        return new CloudStyleRepository();
    }

    public @Bean CloudMapRepository cloudMapRepository() {
        return new CloudMapRepository();
    }
}
