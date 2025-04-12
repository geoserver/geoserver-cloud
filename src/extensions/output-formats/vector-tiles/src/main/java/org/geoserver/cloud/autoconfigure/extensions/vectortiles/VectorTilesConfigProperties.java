/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.vectortiles;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Vector Tiles extension.
 *
 * <p>
 * This class defines the configuration properties for the Vector Tiles extension,
 * with a prefix of "geoserver.extension.vector-tiles".
 *
 * <p>
 * Available properties:
 * <ul>
 *   <li><b>enabled</b>: Whether the Vector Tiles extension as a whole is enabled (default: true)</li>
 *   <li><b>mapbox</b>: Whether the MapBox vector tiles format is enabled (default: true)</li>
 *   <li><b>geojson</b>: Whether the GeoJSON vector tiles format is enabled (default: true)</li>
 *   <li><b>topojson</b>: Whether the TopoJSON vector tiles format is enabled (default: true)</li>
 * </ul>
 *
 * <p>
 * Example YAML configuration:
 * <pre>{@code
 * geoserver:
 *   extension:
 *     vector-tiles:
 *       enabled: true
 *       mapbox: true
 *       geojson: true
 *       topojson: true
 * }</pre>
 *
 * @since 2.27.0
 */
@ConfigurationProperties(prefix = VectorTilesConfigProperties.PREFIX)
public @Data class VectorTilesConfigProperties {

    /** Configuration prefix for vector tiles properties */
    static final String PREFIX = "geoserver.extension.vector-tiles";

    /**
     * Whether the Vector Tiles extension is enabled.
     * When false, all vector tile formats will be disabled regardless of their individual settings.
     */
    private boolean enabled = true;

    /**
     * Whether MapBox vector tiles format is enabled.
     * Controls the availability of the application/vnd.mapbox-vector-tile output format.
     */
    private boolean mapbox = true;

    /**
     * Whether GeoJSON vector tiles format is enabled.
     * Controls the availability of the application/json;type=geojson output format.
     */
    private boolean geojson = true;

    /**
     * Whether TopoJSON vector tiles format is enabled.
     * Controls the availability of the application/json;type=topojson output format.
     */
    private boolean topojson = true;

    /**
     * Checks if any vector tile format is enabled.
     *
     * <p>
     * This method is used to determine the overall enabled state of the Vector Tiles extension.
     * The extension is considered enabled if the main enabled flag is true and at least one
     * of the vector tile formats is enabled.
     *
     * @return true if at least one vector tile format is enabled, false otherwise
     */
    public boolean anyEnabled() {
        return enabled && (mapbox || geojson || topojson);
    }
}
