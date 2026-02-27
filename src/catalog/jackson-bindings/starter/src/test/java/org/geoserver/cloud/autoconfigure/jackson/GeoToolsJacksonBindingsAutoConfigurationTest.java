/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import org.geotools.jackson.databind.filter.GeoToolsFilterModule;
import org.geotools.jackson.databind.geojson.GeoToolsGeoJsonModule;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class GeoToolsJacksonBindingsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    GeoToolsJacksonBindingsAutoConfiguration.class, JacksonAutoConfiguration.class));

    @Test
    void testObjectMapper() {
        // Spring Boot 4 creates multiple ObjectMapper beans (xmlMapper, jacksonJsonMapper)
        this.contextRunner.run(context -> assertThat(context).hasBean("jacksonJsonMapper"));
    }

    @Test
    void testFilterModuleAutoConfiguration() {
        this.contextRunner.run(context -> assertThat(context).hasSingleBean(GeoToolsFilterModule.class));
    }

    @Test
    void testGeoJsonModuleAutoConfiguration() {
        this.contextRunner.run(context -> assertThat(context).hasSingleBean(GeoToolsGeoJsonModule.class));
    }

    @Test
    void testFilterModuleNotInClassPath() {
        this.contextRunner
                .withClassLoader(new FilteredClassLoader(GeoToolsFilterModule.class))
                .run(context -> assertThat(context).doesNotHaveBean(GeoToolsFilterModule.class));
    }

    @Test
    void testGeoJsonModuleNotInClassPath() {
        this.contextRunner
                .withClassLoader(new FilteredClassLoader(GeoToolsFilterModule.class))
                .run(context -> assertThat(context).doesNotHaveBean(GeoToolsGeoJsonModule.class));
    }
}
