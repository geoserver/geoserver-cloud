/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.ogcapi.core;

import org.geoserver.ogcapi.APIConfigurationSupport;
import org.geoserver.rest.RestConfiguration;
import org.geoserver.spring.config.annotations.ComponentScanStrategy;
import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Provides all components of {@code gs-ogcapi-core} jar's {@code applicationContext.xml} but avoiding the too wide
 * component scan on package {@code org.geoserver.ogcapi}.
 *
 * <p>We should make the {@code gs-ogcapi-core} module upstream avoid doing a catch-all component scan, since concrete
 * extensions (e.g. gs-ogcapi-features) will make a component scan on their specific packages (e.g.
 * {@code org.geoserver.ogcapi.v1.features}).
 *
 * <p>{@link RestConfiguration} is imported because {@link APIConfigurationSupport} extends it and requires the
 * {@code mvcContentNegotiationManager} bean it provides (via
 * {@link org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport}).
 *
 * @see RestConfiguration
 * @see OgcApiCoreConfiguration_Generated
 * @see OgcApiCoreConfiguration_Generated.ComponentScannedBeans
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(
        locations = "jar:gs-ogcapi-core-.*!/applicationContext.xml",
        componentScanStrategy = ComponentScanStrategy.GENERATE,
        // Exclude component-scan of org.geoserver.ogcapi.v1.features.* classes
        excludes = "org\\.geoserver\\.ogcapi\\.v1\\.features\\..*")
@Import({RestConfiguration.class, OgcApiCoreConfiguration_Generated.class})
public class OgcApiCoreConfiguration {}
