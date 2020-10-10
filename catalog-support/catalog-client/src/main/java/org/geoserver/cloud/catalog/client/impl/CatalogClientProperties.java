/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.impl;

import java.io.File;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties bean to use the {@code catalog-service} micro-service client back-end
 * and can be used as a {@link ConfigurationProperties @ConfigurationProperties}
 */
public @Data class CatalogClientProperties {
    private boolean enabled;

    private String url;

    private File cacheDirectory;
}
