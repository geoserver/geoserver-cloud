/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.logging.mdc;

import org.geoserver.cloud.logging.mdc.config.GeoServerMdcConfigProperties;
import org.geoserver.cloud.logging.mdc.ows.OWSMdcDispatcherCallback;
import org.geoserver.ows.DispatcherCallback;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link AutoConfiguration @AutoConfiguration} to enable logging MDC (Mapped Diagnostic Context)
 * @SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
 * for GeoServer OWS requests.
 * <p>
 * This configuration automatically sets up the {@link OWSMdcDispatcherCallback} for GeoServer
 * applications, enabling MDC enrichment for OGC Web Service (OWS) requests. The callback
 * adds service and operation information to the MDC, making it available to all logging
 * statements during request processing.
 * <p>
 * The configuration activates only when the following conditions are met:
 * <ul>
 *   <li>The application is a Servlet web application ({@code spring.main.web-application-type=servlet})</li>
 *   <li>GeoServer's {@code Dispatcher} class is on the classpath</li>
 *   <li>Spring Web MVC's {@link org.springframework.web.servlet.mvc.AbstractController} is on the classpath</li>
 * </ul>
 * <p>
 * When active, this configuration creates an {@link OWSMdcDispatcherCallback} bean that integrates
 * with GeoServer's request dispatching process to enrich the MDC with OWS-specific information.
 *
 * @see OWSMdcDispatcherCallback
 * @see GeoServerMdcConfigProperties
 */
@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@EnableConfigurationProperties(GeoServerMdcConfigProperties.class)
@ConditionalOnClass({
    DispatcherCallback.class,
    // from spring-webmvc, required by Dispatcher.class
    org.springframework.web.bind.annotation.RequestMapping.class
})
@ConditionalOnWebApplication(type = Type.SERVLET)
public class GeoServerDispatcherMDCAutoConfiguration {

    /**
     * Creates the OWSMdcDispatcherCallback bean for GeoServer applications.
     * <p>
     * This bean is responsible for adding OWS-specific information to the MDC during
     * GeoServer request processing. It's configured with the OWS-specific settings from
     * the {@link GeoServerMdcConfigProperties}.
     *
     * @param config the GeoServer MDC configuration properties
     * @return the configured OWSMdcDispatcherCallback bean
     */
    @Bean
    OWSMdcDispatcherCallback mdcDispatcherCallback(GeoServerMdcConfigProperties config) {
        return new OWSMdcDispatcherCallback(config.getOws());
    }
}
