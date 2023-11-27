/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.main;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.SLDPackageHandler;
import org.geoserver.cloud.config.catalog.backend.core.GeoServerBackendConfigurer;
import org.geoserver.config.CatalogTimeStampUpdater;
import org.geoserver.platform.resource.SimpleResourceNotificationDispatcher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A pure java configuration for the GeoServer {@link Catalog} that can be used in a WebFlux web
 * application by avoiding any reference to spring-webmvc classes (like {@code
 * org.geoserver.ows.Dispatcher} which is an mvc AbstractController).
 *
 * <p>Note this configuration is intended to be used solely in WebFlux based services. All catalog
 * and config backend configuration is relied upon en enabled {@link GeoServerBackendConfigurer}
 * {@code @Configuration}, loaded either explicitly or, preferably, through one of the
 * auto-configurations provided in {@code starter-catalog-backends}
 */
@Configuration
public class GeoServerMainModuleConfiguration {

    // TODO: revisit, provide an appropriate notification dispatcher in for the event bus
    @Bean
    org.geoserver.platform.resource.ResourceNotificationDispatcher
            resourceNotificationDispatcher() {
        return new SimpleResourceNotificationDispatcher();
    }

    @Bean
    CatalogTimeStampUpdater catalogTimeStampUpdater(@Qualifier("catalog") Catalog catalog) {
        return new CatalogTimeStampUpdater(catalog);
    }

    // <bean id="sldHandler" class="org.geoserver.catalog.SLDHandler"/>
    // <bean id="sldPackageHandler" class="org.geoserver.catalog.SLDPackageHandler">
    // <constructor-arg ref="sldHandler"/>
    // </bean>
    @Bean
    SLDHandler sldHandler() {
        return new SLDHandler();
    }

    @Bean
    SLDPackageHandler sldPackageHandler() {
        return new SLDPackageHandlerHack(sldHandler());
    }

    /** HACK: Class to overcome the protected access to SLDPackageHandler's constructor */
    private static class SLDPackageHandlerHack extends SLDPackageHandler {
        protected SLDPackageHandlerHack(SLDHandler sldHandler) {
            super(sldHandler);
        }
    }
}
