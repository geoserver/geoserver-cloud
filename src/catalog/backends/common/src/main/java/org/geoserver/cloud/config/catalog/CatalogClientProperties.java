/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog;

import lombok.Data;
import lombok.Generated;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;

/**
 * Configuration properties bean to use the {@code catalog-service} micro-service client back-end
 * and can be used as a {@link ConfigurationProperties @ConfigurationProperties}
 */
@Generated
@ConfigurationProperties(prefix = "geoserver.backend.catalog-service")
public @Data class CatalogClientProperties {
    private boolean enabled;
    private String url;
    private File cacheDirectory;
}
