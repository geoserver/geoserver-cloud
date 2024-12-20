/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.logging.mdc;

import org.geoserver.cloud.logging.mdc.config.MDCConfigProperties;
import org.geoserver.cloud.logging.mdc.ows.OWSMdcDispatcherCallback;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.DispatcherCallback;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link AutoConfiguration @AutoConfiguration} to enable logging MDC (Mapped Diagnostic Context)
 * for the GeoSever {@link Dispatcher} events using a {@link DispatcherCallback}
 *
 * @see OWSMdcDispatcherCallback
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({
    Dispatcher.class,
    // from spring-webmvc, required by Dispatcher.class
    org.springframework.web.servlet.mvc.AbstractController.class
})
@ConditionalOnWebApplication(type = Type.SERVLET)
class GeoServerDispatcherMDCConfiguration {

    @Bean
    OWSMdcDispatcherCallback mdcDispatcherCallback(MDCConfigProperties config) {
        return new OWSMdcDispatcherCallback(config.getGeoserver().getOws());
    }
}
