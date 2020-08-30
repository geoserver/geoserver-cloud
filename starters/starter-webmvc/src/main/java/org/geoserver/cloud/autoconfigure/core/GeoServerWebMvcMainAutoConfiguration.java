/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.core;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.autoconfigure.catalog.plugin.PluggableCatalogBackendAutoconfiguration;
import org.geoserver.cloud.config.main.GeoServerMainConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Autoconfiguration for GeoServer's main module on servlet/webmvc applications */
@Configuration
@AutoConfigureAfter(PluggableCatalogBackendAutoconfiguration.class)
@ConditionalOnClass(value = Catalog.class)
@Import({GeoServerMainConfiguration.class})
public class GeoServerWebMvcMainAutoConfiguration {}
