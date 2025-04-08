/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.mapboxstyling;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for MapBox Styling extension.
 *
 * <p>
 * Available properties:
 *
 * <pre>{@code
 * geoserver:
 *   extension:
 *     mapbox-styling:
 *       enabled: true
 * }</pre>
 *
 * @since 2.27.0
 */
@ConfigurationProperties(prefix = "geoserver.extension.mapbox-styling")
public @Data class MapBoxStylingConfigProperties {

    static final boolean DEFAULT = true;

    /**
     * Whether the MapBox Styling extension is enabled.
     */
    private boolean enabled = DEFAULT;
}
