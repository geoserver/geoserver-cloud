/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.core;

import org.geoserver.cloud.autoconfigure.catalog.GeoSeverBackendAutoConfiguration;
import org.geoserver.cloud.config.main.GeoServerMainConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Autoconfiguration for GeoServer's main module on servlet/webmvc applications */
@Configuration
@AutoConfigureAfter(GeoSeverBackendAutoConfiguration.class)
@Import({GeoServerMainConfiguration.class})
public class GeoServerWebMvcMainAutoConfiguration {}
