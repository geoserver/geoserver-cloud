/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.datadirectory;

import lombok.Data;

import org.geoserver.cloud.autoconfigure.catalog.backend.datadir.DataDirectoryAutoConfiguration;
import org.geoserver.cloud.config.catalog.backend.core.GeoServerBackendConfigurer;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * Configuration properties to use GeoServer's traditional, file-system based data-directory as the
 * {@link GeoServerBackendConfigurer catalog and configuration backend} through the {@link
 * DataDirectoryAutoConfiguration} auto-configuration.
 */
@ConfigurationProperties(prefix = "geoserver.backend.data-directory")
@Data
public class DataDirectoryProperties {

    private boolean enabled;
    private Path location;
    private boolean parallelLoader = true;
}
