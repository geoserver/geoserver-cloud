/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactivefeign.spring.config.ReactiveFeignAutoConfiguration;

public class ReactiveCatalogApiClientConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ReactiveFeignAutoConfiguration.class,
                    ReactiveCatalogApiClientConfiguration.class));

    public @Test void workspaceClient() {
        this.contextRunner.run(context -> assertThat(context).hasSingleBean(ReactiveCatalogClient.class));
    }
}
