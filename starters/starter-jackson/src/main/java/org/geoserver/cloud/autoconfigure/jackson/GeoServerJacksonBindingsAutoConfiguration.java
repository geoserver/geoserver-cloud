/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.jackson;

import com.fasterxml.jackson.databind.Module;
import org.geoserver.jackson.databind.catalog.GeoServerCatalogModule;
import org.geoserver.jackson.databind.config.GeoServerConfigModule;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring boot {@link EnableAutoConfiguration @EnableAutoConfiguration} to register GeoServer
 * jackson databind {@link Module modules}.
 *
 * <p>Configuration enablement is conditional on the presence of {@link GeoServerCatalogModule} on
 * the classpath. Add an explicit dependency on {@code gs-cloud-catalog-support:gs-jackson-bindings}
 * to use it.
 *
 * <p>Spring-boot's default auto configuration does not register all modules in the classpath,
 * despite them being register-able through Jackson's SPI; a configuration like this is needed to
 * set up the application required ones.
 */
@Configuration
@AutoConfigureAfter(GeoToolsJacksonBindingsAutoConfiguration.class)
@ConditionalOnClass(GeoServerCatalogModule.class)
public class GeoServerJacksonBindingsAutoConfiguration {

    @ConditionalOnMissingBean(GeoServerCatalogModule.class)
    public @Bean GeoServerCatalogModule geoServerCatalogJacksonModule() {
        return new GeoServerCatalogModule();
    }

    @ConditionalOnMissingBean(GeoServerConfigModule.class)
    public @Bean GeoServerConfigModule geoServerConfigJacksonModule() {
        return new GeoServerConfigModule();
    }
}
