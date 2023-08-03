/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.core;

import org.geoserver.cloud.autoconfigure.catalog.backend.core.GeoServerBackendAutoConfiguration;
import org.geoserver.cloud.config.main.GeoServerMainModuleConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/** Autoconfiguration for GeoServer's main module on servlet/webmvc applications */
@AutoConfiguration(after = GeoServerBackendAutoConfiguration.class)
@Import({GeoServerMainModuleConfiguration.class})
public class GeoServerWebMvcMainAutoConfiguration {}
