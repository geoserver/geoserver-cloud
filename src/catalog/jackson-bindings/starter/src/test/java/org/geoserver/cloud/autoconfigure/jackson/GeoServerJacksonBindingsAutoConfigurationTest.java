/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.jackson.databind.catalog.GeoServerCatalogModule;
import org.geoserver.jackson.databind.config.GeoServerConfigModule;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class GeoServerJacksonBindingsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    GeoServerJacksonBindingsAutoConfiguration.class, JacksonAutoConfiguration.class));

    @Test
    void testObjectMapper() {
        // Spring Boot 4 creates multiple ObjectMapper beans (xmlMapper, jacksonJsonMapper)
        this.contextRunner.run(context -> assertThat(context).hasBean("jacksonJsonMapper"));
    }

    @Test
    void testCatalogModuleAutoConfiguration() {
        this.contextRunner.run(context -> assertThat(context).hasSingleBean(GeoServerCatalogModule.class));
    }

    @Test
    void testConfigModuleAutoConfiguration() {
        this.contextRunner.run(context -> assertThat(context).hasSingleBean(GeoServerConfigModule.class));
    }
}
