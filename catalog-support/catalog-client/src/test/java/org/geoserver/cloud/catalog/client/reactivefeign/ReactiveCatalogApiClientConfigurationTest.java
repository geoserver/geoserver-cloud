/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactivefeign.spring.config.ReactiveFeignAutoConfiguration;

public class ReactiveCatalogApiClientConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    ReactiveFeignAutoConfiguration.class,
                                    WebClientAutoConfiguration.class))
                    .withConfiguration(
                            UserConfigurations.of(ReactiveCatalogApiClientConfiguration.class));

    public @Test void testReactiveCatalogClientIsLoaded() {
        this.contextRunner.run(
                context -> {
                    assertThat(context).hasSingleBean(ReactiveCatalogClient.class);
                    ReactiveCatalogClient client = context.getBean(ReactiveCatalogClient.class);
                    assertNotNull(client);
                });
    }
}
