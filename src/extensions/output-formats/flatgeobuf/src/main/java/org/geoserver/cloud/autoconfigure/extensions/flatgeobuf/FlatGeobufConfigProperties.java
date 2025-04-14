/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.flatgeobuf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for FlatGeobuf extension.
 *
 * <p>
 * This class defines the configuration properties for the FlatGeobuf extension,
 * with a prefix of "geoserver.extension.flatgeobuf".
 *
 * <p>
 * Available properties:
 * <ul>
 *   <li><b>enabled</b>: Whether the FlatGeobuf extension is enabled (default: true)</li>
 * </ul>
 *
 * <p>
 * Example YAML configuration:
 * <pre>{@code
 * geoserver:
 *   extension:
 *     flatgeobuf:
 *       enabled: true
 * }</pre>
 *
 * @since 2.27.0
 */
@ConfigurationProperties(prefix = FlatGeobufConfigProperties.PREFIX)
public @Data class FlatGeobufConfigProperties {

    /** Configuration prefix for FlatGeobuf properties */
    static final String PREFIX = "geoserver.extension.flatgeobuf";

    /**
     * Whether the FlatGeobuf extension is enabled.
     * Controls the availability of the FlatGeobuf output format for WFS.
     */
    private boolean enabled = true;
}
