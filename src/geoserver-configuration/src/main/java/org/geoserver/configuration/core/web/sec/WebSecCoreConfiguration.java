/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.web.sec;

import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration for the GeoServer core security web UI components.
 *
 * <p>
 * This configuration registers the core security web UI components like panel
 * info classes for configuration through the GeoServer web admin interface.
 *
 * @since 2.28.0, priorly imported directly in WebSecurityAutoConfiguration
 * @see WebSecCoreConfiguration_Generated
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(locations = "jar:gs-web-sec-core-.*!/applicationContext.xml")
@Import({WebSecCoreConfiguration_Generated.class})
public class WebSecCoreConfiguration {}
