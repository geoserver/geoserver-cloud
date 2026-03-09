/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.web.gwc;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheRestConfigEnabled;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnWebUIEnabled;
import org.geoserver.cloud.gwc.config.core.CloudGwcUrlHandlerMapping;
import org.geoserver.cloud.gwc.config.core.GeoWebCacheConfigurationProperties;
import org.geoserver.gwc.controller.GwcUrlHandlerMapping;
import org.geowebcache.GeoWebCacheDispatcher;
import org.geowebcache.rest.controller.ByteStreamController;
import org.gwc.web.rest.GeoWebCacheController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnWebUIEnabled
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.web.gwc")
public class GeoWebCacheUIAutoConfiguration {

    public @PostConstruct void log() {
        log.info("{} enabled", GeoWebCacheConfigurationProperties.WEBUI_ENABLED);
    }

    @Bean
    @SuppressWarnings({"deprecation", "java:S1874"})
    GwcUrlHandlerMapping gwcDemoUrlHandlerMapping(Catalog catalog) {
        GwcUrlHandlerMapping handler = new CloudGwcUrlHandlerMapping(catalog, "/gwc/demo");
        handler.setAlwaysUseFullPath(true);
        handler.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return handler;
    }

    @Bean
    @SuppressWarnings({"deprecation", "java:S1874"})
    GwcUrlHandlerMapping gwcRestWebUrlHandlerMapping(Catalog catalog) {
        GwcUrlHandlerMapping handler = new CloudGwcUrlHandlerMapping(catalog, "/gwc/rest/web");
        handler.setAlwaysUseFullPath(true);
        handler.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return handler;
    }

    @Bean
    GeoWebCacheController gwcController(GeoWebCacheDispatcher gwcDispatcher) {
        return new GeoWebCacheController(gwcDispatcher);
    }

    /**
     * Provide a handler for static web resources if missing, for example, because {@link
     * ConditionalOnGeoWebCacheRestConfigEnabled} is disabled
     */
    @Bean
    @ConditionalOnMissingBean(ByteStreamController.class)
    ByteStreamController byteStreamController() {
        return new ByteStreamController();
    }
}
