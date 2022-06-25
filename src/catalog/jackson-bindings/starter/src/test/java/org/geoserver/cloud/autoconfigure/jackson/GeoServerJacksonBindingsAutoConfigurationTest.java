/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.HamcrestCondition.matching;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.assertj.core.api.Condition;
import org.geoserver.jackson.databind.catalog.GeoServerCatalogModule;
import org.geoserver.jackson.databind.config.GeoServerConfigModule;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Set;

public class GeoServerJacksonBindingsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    GeoServerJacksonBindingsAutoConfiguration.class,
                                    JacksonAutoConfiguration.class));

    public @Test void testObjectMapper() {
        this.contextRunner.run(context -> assertThat(context).hasSingleBean(ObjectMapper.class));
        Condition<? super Set<Object>> condition =
                matching(
                        Matchers.hasItems(
                                new GeoServerCatalogModule().getTypeId(),
                                new GeoServerConfigModule().getTypeId()));
        this.contextRunner.run(
                context ->
                        assertThat(context)
                                .getBean(ObjectMapper.class)
                                .extracting(ObjectMapper::getRegisteredModuleIds)
                                .has(condition));
    }
}
