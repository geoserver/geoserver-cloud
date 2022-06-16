/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.core;

import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.ResourceStoreFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

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
 */
public interface GeoServerBackendConfigurer {

    @Bean
    ExtendedCatalogFacade catalogFacade();

    @Bean
    GeoServerLoader geoServerLoaderImpl();

    @Bean
    GeoServerFacade geoserverFacade();

    /**
     * {@link ResourceStore} named {@code resourceStoreImpl}, as looked up in the application
     * context by {@link ResourceStoreFactory}. With this, we don't need a bean called
     * "dataDirectoryResourceStore" at all.
     */
    @Bean
    ResourceStore resourceStoreImpl();

    @Bean
    GeoServerResourceLoader resourceLoader();
}
