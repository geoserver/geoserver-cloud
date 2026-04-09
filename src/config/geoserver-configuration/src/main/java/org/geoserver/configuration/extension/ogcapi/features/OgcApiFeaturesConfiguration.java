/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.ogcapi.features;

import org.geoserver.configuration.extension.ogcapi.core.OgcApiCoreConfiguration;
import org.geoserver.spring.config.annotations.ComponentScanStrategy;
import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration class for OGC API Features service.
 *
 * <p>Uses {@code componentScanStrategy = IGNORE} to avoid the XML's component scan on
 * {@code org.geoserver.ogcapi.v1.features}. The stereotyped components are registered explicitly below.
 *
 * @see OgcApiCoreConfiguration
 * @see OgcApiFeaturesConfiguration_Generated
 * @see OgcApiFeaturesConfiguration_Generated.ComponentScannedBeans
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(
        locations = "jar:gs-ogcapi-features-.*!/applicationContext.xml",
        componentScanStrategy = ComponentScanStrategy.GENERATE)
@Import({OgcApiCoreConfiguration.class, OgcApiFeaturesConfiguration_Generated.class})
@SuppressWarnings("java:S6830")
public class OgcApiFeaturesConfiguration {}
