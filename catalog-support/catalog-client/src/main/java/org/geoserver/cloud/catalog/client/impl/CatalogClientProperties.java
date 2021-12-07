/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.impl;

import java.io.File;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties bean to use the {@code catalog-service} micro-service client back-end
 * and can be used as a {@link ConfigurationProperties @ConfigurationProperties}
 */
// @ConfigurationProperties(prefix = "geoserver.backend.catalog-service")
public @Data class CatalogClientProperties {

    @Value("${geoserver.backend.catalog-service.enabled:false}")
    private boolean enabled;

    @Value("${geoserver.backend.catalog-service.url:}")
    private String url;

    @Value("${geoserver.backend.catalog-service.cache-directory:}")
    private File cacheDirectory;
}
