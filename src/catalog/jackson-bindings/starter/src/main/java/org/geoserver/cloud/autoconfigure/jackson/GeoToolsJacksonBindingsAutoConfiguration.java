/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.jackson;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.geotools.jackson.databind.filter.GeoToolsFilterModule;
import org.geotools.jackson.databind.geojson.GeoToolsGeoJsonModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Spring boot {@link EnableAutoConfiguration @EnableAutoConfiguration} to register GeoTools and
 * jackson databind {@link Module modules}.
 *
 * <p>Configuration enablement is conditional on the presence of {@link GeoToolsFilterModule} on the
 * classpath. Add an explicit dependency on {@code gs-cloud-core:gt-jackson-bindings} to use it.
 *
 * <p>Spring-boot's default auto configuration does not register all modules in the classpath,
 * despite them being register-able through Jackson's SPI; a configuration like this is needed to
 * set up the application required ones.
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnClass(GeoToolsFilterModule.class)
public class GeoToolsJacksonBindingsAutoConfiguration {

    @ConditionalOnMissingBean(JavaTimeModule.class)
    @Bean
    JavaTimeModule javaTimeModule() {
        return new JavaTimeModule();
    }

    @Bean
    GeoToolsGeoJsonModule geoToolsGeoJsonModule() {
        return new GeoToolsGeoJsonModule();
    }

    @Bean
    GeoToolsFilterModule geoToolsFilterModule() {
        return new GeoToolsFilterModule();
    }
}
