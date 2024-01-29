/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.web.gwc;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnWebUIEnabled;
import org.geoserver.cloud.gwc.config.core.GeoWebCacheConfigurationProperties;
import org.geoserver.cloud.virtualservice.VirtualServiceVerifier;
import org.geoserver.gwc.controller.GwcUrlHandlerMapping;
import org.geoserver.ows.Dispatcher;
import org.geowebcache.GeoWebCacheDispatcher;
import org.geowebcache.rest.controller.ByteStreamController;
import org.gwc.web.rest.GeoWebCacheController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;

@AutoConfiguration
@ConditionalOnWebUIEnabled
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.web.gwc")
public class GeoWebCacheUIAutoConfiguration {

    public @PostConstruct void log() {
        log.info("{} enabled", GeoWebCacheConfigurationProperties.WEBUI_ENABLED);
    }

    @Bean
    GeoWebCacheController gwcController(
            Dispatcher geoserverDispatcher,
            GeoWebCacheDispatcher geoWebCacheDispatcher,
            VirtualServiceVerifier verifier) {
        return new GeoWebCacheController(geoserverDispatcher, geoWebCacheDispatcher, verifier);
    }

    /** ConditionalOnGeoWebCacheRestConfigEnabled} is disabled */
    @Bean
    @ConditionalOnMissingBean(ByteStreamController.class)
    ByteStreamController byteStreamController() {
        return new ByteStreamController();
    }

    @Bean
    VirtualServiceVerifier virtualServiceVerifier(@Qualifier("rawCatalog") Catalog catalog) {
        return new VirtualServiceVerifier(catalog);
    }

    /**
     * GS's src/web/gwc/src/main/java/applicationContext.xml
     * <!-- Used for workspace-based demo requests so the requests to GS stay workspace-based -->
     * <bean id="gwcDemoUrlHandlerMapping"
     * class="org.geoserver.gwc.controller.GwcUrlHandlerMapping"> <constructor-arg ref="catalog" />
     * <constructor-arg value="/gwc/demo"/> <property name="alwaysUseFullPath" value="true" />
     * <property name="order" value="10" /> </bean>
     * <!-- Used for workspace-based web requests (i.e. for rest/web/openlayer/ol.js) -->
     * <bean id="gwcRestWebUrlHandlerMapping"
     * class="org.geoserver.gwc.controller.GwcUrlHandlerMapping"> <constructor-arg ref="catalog" />
     * <constructor-arg type="java.lang.String" value="/gwc/rest/web"/> <property
     * name="alwaysUseFullPath" value="true" /> <property name="order" value="10" /> </bean>
     */
    @Bean
    @Qualifier("gwcDemoUrlHandlerMapping")
    GwcUrlHandlerMapping gwcDemoUrlHandlerMapping(@Qualifier("rawCatalog") Catalog catalog) {
        GwcUrlHandlerMapping handler = new GwcUrlHandlerMapping(catalog, "/gwc/demo");
        handler.setAlwaysUseFullPath(true);
        handler.setOrder(10);

        return handler;
    }

    /*
    @Bean
    WebMvcRegistrations gwcDemoUrlHandlerMappingRegistrations(
            @Qualifier("gwcDemoUrlHandlerMapping") GwcUrlHandlerMapping handler) {
        return new WebMvcRegistrations() {
            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                return handler;
            }
        };
    }*/

    @Bean
    @Qualifier("gwcRestWebUrlHandlerMapping")
    GwcUrlHandlerMapping gwcRestWebUrlHandlerMapping(@Qualifier("rawCatalog") Catalog catalog) {
        GwcUrlHandlerMapping handler = new GwcUrlHandlerMapping(catalog, "/gwc/rest/web");
        handler.setAlwaysUseFullPath(true);
        handler.setOrder(10);

        return handler;
    }

    /*
    @Bean
    WebMvcRegistrations gwcRestWebUrlHandlerMappingRegistrations(
            @Qualifier("gwcRestWebUrlHandlerMapping") GwcUrlHandlerMapping handler) {
        return new WebMvcRegistrations() {
            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                return handler;
            }
        };
    }*/
}
