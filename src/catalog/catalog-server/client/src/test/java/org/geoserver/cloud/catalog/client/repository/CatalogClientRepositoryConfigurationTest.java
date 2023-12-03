/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveCatalogApiClientConfiguration;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveCatalogClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import reactivefeign.spring.config.ReactiveFeignAutoConfiguration;

class CatalogClientRepositoryConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withAllowBeanDefinitionOverriding(true)
                    .withPropertyValues("reactive.feign.loadbalancer.enabled=false")
                    .withConfiguration(
                            AutoConfigurations.of(
                                    ReactiveFeignAutoConfiguration.class,
                                    WebClientAutoConfiguration.class))
                    .withConfiguration(
                            UserConfigurations.of(
                                    ReactiveCatalogApiClientConfiguration.class,
                                    CatalogClientRepositoryConfiguration.class));

    void verify(Class<? extends CatalogClientRepository<?>> repoType) {
        this.contextRunner.run(context -> assertThat(context).hasSingleBean(repoType));

        this.contextRunner.run(
                context ->
                        assertThat(context)
                                .getBean(repoType)
                                .satisfies(
                                        r -> {
                                            ReactiveCatalogClient client = r.client();
                                            assertNotNull(client);
                                        }));
    }

    @Test
    void workspaceRepository() {
        verify(CatalogClientWorkspaceRepository.class);
    }

    @Test
    void namespaceRepository() {
        verify(CatalogClientNamespaceRepository.class);
    }

    @Test
    void storeRepository() {
        verify(CatalogClientStoreRepository.class);
    }

    @Test
    void resourceRepository() {
        verify(CatalogClientResourceRepository.class);
    }

    @Test
    void layerRepository() {
        verify(CatalogClientLayerRepository.class);
    }

    @Test
    void layerGroupRepository() {
        verify(CatalogClientLayerGroupRepository.class);
    }

    @Test
    void styleRepository() {
        verify(CatalogClientNamespaceRepository.class);
    }

    @Test
    void mapRepository() {
        verify(CatalogClientMapRepository.class);
    }
}
