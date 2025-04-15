/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.configuration.ogcapi.core;

import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.springframework.context.annotation.Configuration;

/**
 * Provides all components of {@code gs-web-ogcapi} jar's
 * {@code applicationContext.xml}
 * <p>
 * This is not an auto-configuration, but meant to be included by concrete APIs
 * auto-configurations, in order to avoid these core OGC API components
 * contributed to unrelated services.
 */
@ConditionalOnGeoServerWebUI
@Configuration(proxyBeanMethods = false)
@ImportFilteredResource({"jar:gs-web-ogcapi-.*!/applicationContext.xml"})
public class OgcApiCoreWebConfiguration {}
