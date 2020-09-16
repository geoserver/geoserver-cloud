package org.geoserver.cloud.autoconfigure.jackson;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.HamcrestCondition.matching;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Set;
import org.assertj.core.api.Condition;
import org.geotools.jackson.databind.filter.GeoToolsFilterModule;
import org.geotools.jackson.databind.geojson.GeoToolsGeoJsonModule;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

public class GeoToolsJacksonBindingsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    GeoToolsJacksonBindingsAutoConfiguration.class,
                                    JacksonAutoConfiguration.class));

    public @Test void testObjectMapper() {
        this.contextRunner.run(context -> assertThat(context).hasSingleBean(ObjectMapper.class));
        Condition<? super Set<Object>> condition =
                matching(
                        Matchers.hasItems(
                                GeoToolsFilterModule.class.getName(),
                                GeoToolsGeoJsonModule.class.getName(),
                                JavaTimeModule.class.getName()));
        this.contextRunner.run(
                context ->
                        assertThat(context)
                                .getBean(ObjectMapper.class)
                                .extracting(ObjectMapper::getRegisteredModuleIds)
                                .has(condition));
    }

    public @Test void testFilterModuleAutoConfiguration() {
        this.contextRunner.run(
                context -> assertThat(context).hasSingleBean(GeoToolsFilterModule.class));
    }

    public @Test void testGeoJsonModuleAutoConfiguration() {
        this.contextRunner.run(
                context -> assertThat(context).hasSingleBean(GeoToolsGeoJsonModule.class));
    }

    public @Test void testFilterModuleNotInClassPath() {
        this.contextRunner
                .withClassLoader(new FilteredClassLoader(GeoToolsFilterModule.class))
                .run(context -> assertThat(context).doesNotHaveBean(GeoToolsFilterModule.class));
    }

    public @Test void testGeoJsonModuleNotInClassPath() {
        this.contextRunner
                .withClassLoader(new FilteredClassLoader(GeoToolsFilterModule.class))
                .run(context -> assertThat(context).doesNotHaveBean(GeoToolsGeoJsonModule.class));
    }
}
