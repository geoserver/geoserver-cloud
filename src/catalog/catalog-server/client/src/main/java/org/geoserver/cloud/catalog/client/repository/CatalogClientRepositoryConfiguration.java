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
public class CatalogClientRepositoryConfiguration {

    @Bean
    CatalogClientWorkspaceRepository cloudWorkspaceRepository() {
        return new CatalogClientWorkspaceRepository();
    }

    @Bean
    CatalogClientNamespaceRepository cloudNamespaceRepository() {
        return new CatalogClientNamespaceRepository();
    }

    @Bean
    CatalogClientStoreRepository cloudStoreRepository() {
        return new CatalogClientStoreRepository();
    }

    @Bean
    CatalogClientResourceRepository cloudResourceRepository() {
        return new CatalogClientResourceRepository();
    }

    @Bean
    CatalogClientLayerRepository cloudLayerRepository() {
        return new CatalogClientLayerRepository();
    }

    @Bean
    CatalogClientLayerGroupRepository cloudLayerGroupRepository() {
        return new CatalogClientLayerGroupRepository();
    }

    @Bean
    CatalogClientStyleRepository cloudStyleRepository() {
        return new CatalogClientStyleRepository();
    }

    @Bean
    CatalogClientMapRepository cloudMapRepository() {
        return new CatalogClientMapRepository();
    }
}
