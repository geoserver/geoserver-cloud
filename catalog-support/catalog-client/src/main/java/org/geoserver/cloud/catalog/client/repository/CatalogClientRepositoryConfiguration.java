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

    public @Bean CatalogClientWorkspaceRepository cloudWorkspaceRepository() {
        return new CatalogClientWorkspaceRepository();
    }

    public @Bean CatalogClientNamespaceRepository cloudNamespaceRepository() {
        return new CatalogClientNamespaceRepository();
    }

    public @Bean CatalogClientStoreRepository cloudStoreRepository() {
        return new CatalogClientStoreRepository();
    }

    public @Bean CatalogClientResourceRepository cloudResourceRepository() {
        return new CatalogClientResourceRepository();
    }

    public @Bean CatalogClientLayerRepository cloudLayerRepository() {
        return new CatalogClientLayerRepository();
    }

    public @Bean CatalogClientLayerGroupRepository cloudLayerGroupRepository() {
        return new CatalogClientLayerGroupRepository();
    }

    public @Bean CatalogClientStyleRepository cloudStyleRepository() {
        return new CatalogClientStyleRepository();
    }

    public @Bean CatalogClientMapRepository cloudMapRepository() {
        return new CatalogClientMapRepository();
    }
}
