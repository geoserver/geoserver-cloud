/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.security;

import org.geoserver.cloud.autoconfigure.catalog.GeoServerBackendAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@AutoConfigureAfter(GeoServerBackendAutoConfiguration.class)
@Import({
    GeoServerSecurityEnabledAutoConfiguration.class,
    GeoServerSecurityDisabledAutoConfiguration.class
})
public class GeoServerSecurityAutoConfiguration {}
