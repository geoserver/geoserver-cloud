/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class CatalogRepositoriesConfiguration {

    public @Lazy @Bean CloudWorkspaceRepository cloudWorkspaceRepository() {
        return new CloudWorkspaceRepository();
    }

    public @Lazy @Bean CloudNamespaceRepository cloudNamespaceRepository() {
        return new CloudNamespaceRepository();
    }

    public @Lazy @Bean CloudStoreRepository cloudStoreRepository() {
        return new CloudStoreRepository();
    }

    public @Lazy @Bean CloudResourceRepository cloudResourceRepository() {
        return new CloudResourceRepository();
    }

    public @Lazy @Bean CloudLayerRepository cloudLayerRepository() {
        return new CloudLayerRepository();
    }

    public @Lazy @Bean CloudLayerGroupRepository cloudLayerGroupRepository() {
        return new CloudLayerGroupRepository();
    }

    public @Lazy @Bean CloudStyleRepository cloudStyleRepository() {
        return new CloudStyleRepository();
    }

    public @Lazy @Bean CloudMapRepository cloudMapRepository() {
        return new CloudMapRepository();
    }
}
