/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.HamcrestCondition.matching;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Set;
import org.assertj.core.api.Condition;
import org.geotools.jackson.databind.filter.GeoToolsFilterModule;
import org.geotools.jackson.databind.geojson.GeoToolsGeoJsonModule;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class GeoToolsJacksonBindingsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    GeoToolsJacksonBindingsAutoConfiguration.class, JacksonAutoConfiguration.class));

    @Test
    void testObjectMapper() {
        this.contextRunner.run(context -> assertThat(context).hasSingleBean(ObjectMapper.class));
        Condition<? super Set<Object>> condition = matching(Matchers.hasItems(
                new GeoToolsFilterModule().getTypeId(),
                new GeoToolsGeoJsonModule().getTypeId(),
                new JavaTimeModule().getTypeId()));
        this.contextRunner.run(context -> assertThat(context)
                .getBean(ObjectMapper.class)
                .extracting(ObjectMapper::getRegisteredModuleIds)
                .has(condition));
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
