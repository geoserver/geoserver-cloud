/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.datadirectory;

import java.nio.file.Path;
import lombok.Data;

public @Data class DataDirectoryProperties {
    private boolean enabled;
    private Path location;
}
