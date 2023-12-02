/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;

import reactivefeign.spring.config.ReactiveFeignAutoConfiguration;

class ReactiveCatalogApiClientConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner() //
                    .withAllowBeanDefinitionOverriding(true)
                    .withPropertyValues("reactive.feign.loadbalancer.enabled=false")
                    .withConfiguration(
                            AutoConfigurations.of( //
                                    ReactiveFeignAutoConfiguration.class, //
                                    FeignAutoConfiguration.class, //
                                    WebClientAutoConfiguration.class //
                                    ))
                    .withConfiguration(
                            UserConfigurations.of( //
                                    ReactiveCatalogApiClientConfiguration.class //
                                    ));

    @Test void testReactiveCatalogClientIsLoaded() {
        this.contextRunner.run(
                context -> {
                    assertThat(context).hasSingleBean(ReactiveCatalogClient.class);
                    ReactiveCatalogClient client = context.getBean(ReactiveCatalogClient.class);
                    assertNotNull(client);
                });
    }

    @Test void testReactiveConfigClientIsLoaded() {
        this.contextRunner.run(
                context -> {
                    assertThat(context).hasSingleBean(ReactiveConfigClient.class);
                    ReactiveConfigClient client = context.getBean(ReactiveConfigClient.class);
                    assertNotNull(client);
                });
    }

    @Test void testReactiveResourceStoreClientIsLoaded() {
        this.contextRunner.run(
                context -> {
                    assertThat(context).hasSingleBean(ReactiveResourceStoreClient.class);
                    ReactiveResourceStoreClient client =
                            context.getBean(ReactiveResourceStoreClient.class);
                    assertNotNull(client);
                });
    }
}
