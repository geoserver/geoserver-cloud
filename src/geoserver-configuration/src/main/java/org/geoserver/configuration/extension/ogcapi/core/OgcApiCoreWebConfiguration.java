/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.ogcapi.core;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Provides all components of {@code gs-web-ogcapi} jar's
 * {@code applicationContext.xml}
 * <p>
 * This is not an auto-configuration, but meant to be included by concrete APIs
 * auto-configurations, in order to avoid these core OGC API components
 * contributed to unrelated services.
 */
@Configuration
@TranspileXmlConfig(locations = "jar:gs-web-ogcapi-.*!/applicationContext.xml")
@Import({OgcApiCoreConfiguration.class, OgcApiCoreWebConfiguration_Generated.class})
public class OgcApiCoreWebConfiguration {}
