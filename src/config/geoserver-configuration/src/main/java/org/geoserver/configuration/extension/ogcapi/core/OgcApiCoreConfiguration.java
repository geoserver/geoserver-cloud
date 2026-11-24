/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.ogcapi.core;

import org.geoserver.ogcapi.APIConfigurationSupport;
import org.geoserver.ogcapi.APIDispatcher;
import org.geoserver.spring.config.annotations.ComponentScanStrategy;
import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * Provides all components of {@code gs-ogcapi-core} jar's {@code applicationContext.xml} but avoiding the too wide
 * component scan on package {@code org.geoserver.ogcapi}.
 *
 * <p>We should make the {@code gs-ogcapi-core} module upstream avoid doing a catch-all component scan, since concrete
 * extensions (e.g. gs-ogcapi-features) will make a component scan on their specific packages (e.g.
 * {@code org.geoserver.ogcapi.v1.features}).
 *
 * <p>This configuration consumes Spring MVC, it does not configure it. Whichever context uses it must already hold a
 * {@link WebMvcConfigurationSupport} owner, Spring Boot's auto-configured one or GeoServer's {@code RestConfiguration},
 * because {@link APIDispatcher} reads {@code mvcContentNegotiationManager}, {@code mvcConversionService} and
 * {@code mvcValidator} from the context while it starts.
 *
 * <p>{@link APIConfigurationSupport} extends {@code RestConfiguration} but is deliberately not a configuration class
 * upstream ("Not {@code @Configuration} on purpose, or it will interfere with the REST API"): {@link APIDispatcher}
 * calls {@code requestMappingHandlerAdapter(..)} on it directly to build the callback-aware handler adapter the OGC API
 * dispatches with, which works whoever owns the context's Spring MVC infrastructure. Registering a second
 * {@code WebMvcConfigurationSupport} configuration class here would only make ownership depend on parse order.
 *
 * <p>{@code OGCAPIXStreamPersisterInitializer} and {@code filterConformanceXStreamInitializer} are excluded. They
 * describe the {@code ogcApiLinks}, {@code cql2} and {@code ecql} metadata entries, read and written by all services.
 * {@code OgcApiCoreXStreamPersisterAutoConfiguration} contributes both unconditionally, instead of this service-scoped
 * configuration.
 *
 * @see OgcApiCoreConfiguration_Generated
 * @see OgcApiCoreConfiguration_Generated.ComponentScannedBeans
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(
        locations = "jar:gs-ogcapi-core-.*!/applicationContext.xml",
        componentScanStrategy = ComponentScanStrategy.GENERATE,
        excludes = {
            // Exclude component-scan of org.geoserver.ogcapi.v1.features.* classes
            "org\\.geoserver\\.ogcapi\\.v1\\.features\\..*",
            "org\\.geoserver\\.ogcapi\\.OGCAPIXStreamPersisterInitializer",
            "filterConformanceXStreamInitializer"
        })
@Import(OgcApiCoreConfiguration_Generated.class)
public class OgcApiCoreConfiguration {}
