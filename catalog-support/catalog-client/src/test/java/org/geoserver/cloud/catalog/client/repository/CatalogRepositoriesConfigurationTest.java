/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveCatalogApiClientConfiguration;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveCatalogClient;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactivefeign.spring.config.ReactiveFeignAutoConfiguration;

public class CatalogRepositoriesConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    ReactiveFeignAutoConfiguration.class,
                                    WebClientAutoConfiguration.class))
                    .withConfiguration(
                            UserConfigurations.of(
                                    ReactiveCatalogApiClientConfiguration.class,
                                    CatalogRepositoriesConfiguration.class));

    void verify(Class<? extends CatalogServiceClientRepository<?>> repoType) {
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

    public @Test void workspaceRepository() {
        verify(CloudWorkspaceRepository.class);
    }

    public @Test void namespaceRepository() {
        verify(CloudNamespaceRepository.class);
    }

    public @Test void storeRepository() {
        verify(CloudStoreRepository.class);
    }

    public @Test void resourceRepository() {
        verify(CloudResourceRepository.class);
    }

    public @Test void layerRepository() {
        verify(CloudLayerRepository.class);
    }

    public @Test void layerGroupRepository() {
        verify(CloudLayerGroupRepository.class);
    }

    public @Test void styleRepository() {
        verify(CloudNamespaceRepository.class);
    }

    public @Test void mapRepository() {
        verify(CloudMapRepository.class);
    }
}
