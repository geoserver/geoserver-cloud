/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog;

import lombok.Data;
import lombok.Generated;

import org.geoserver.cloud.autoconfigure.catalog.DataDirectoryAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * Configuration properties to use GeoServer's traditional, file-system based data-directory as the
 * {@link GeoServerBackendConfigurer catalog and configuration backend} through the {@link
 * DataDirectoryAutoConfiguration} auto-configuration.
 */
@Generated
@ConfigurationProperties(prefix = "geoserver.backend.data-directory")
public @Data class DataDirectoryProperties {

    private boolean enabled;
    private Path location;
}
