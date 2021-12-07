/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog;

import org.geoserver.cloud.config.datadirectory.DataDirectoryBackendConfigurer;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.GeoServerLoaderProxy;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Unified provider interface for the complete GeoServer backend storage (catalog and config).
 *
 * <p>If in vanilla GeoServer the default storage is the "data directory", in the "cloud native
 * GeoServer", you need to explicitly enable and configure the backend by means of one of this
 * interface implementations, using traditional spring's externalized configuration properties.
 *
 * <p>An <code>@Configuration</code> annotated class that implements this interface is all a
 * catalog/configuration "plugin" needs to provide to set up GeoServer's catalog and configuration
 * storage for a particular backend.
 *
 * <p>Such configuration class should be included as a spring-boot auto-configuration by adding it
 * to the {@code org.springframework.boot.autoconfigure.EnableAutoConfiguration} list of
 * auto-configuration classes in {@code META-INF/spring.factories}, and should implement a means to
 * enable/disable itself based on the required criteria, for example, using {@link
 * ConditionalOnProperty @ConditionalOnProperty}, {@link ConditionalOnClass @ConditionalOnClass},
 * etc.
 *
 * @see DataDirectoryBackendConfigurer
 */
@Configuration(proxyBeanMethods = true)
@EnableConfigurationProperties
public interface GeoServerBackendConfigurer
        extends GeoServerCatalogConfigurer, GeoServerConfigConfigurer {

    //    @ConfigurationProperties(prefix = "geoserver.backend")
    //    default @Bean GeoServerBackendProperties geoServerBackendProperties() {
    //        return new GeoServerBackendProperties();
    //    }
    //
    //    @ConfigurationProperties(prefix = "geoserver.catalog")
    //    default @Bean CatalogProperties geoServerCatalogProperties() {
    //        return new CatalogProperties();
    //    }

    public @Bean GeoServerLoader geoServerLoaderImpl();

    @DependsOn("geoServerLoaderImpl")
    default @Bean GeoServerLoaderProxy geoServerLoader() {
        return new GeoServerLoaderProxy(resourceLoader());
    }

    default @Bean GeoServerExtensions extensions() {
        return new GeoServerExtensions();
    }

    @ConditionalOnMissingBean(name = "xstreamPersisterFactory")
    default @Bean XStreamPersisterFactory xstreamPersisterFactory() {
        return new XStreamPersisterFactory();
    }
}
