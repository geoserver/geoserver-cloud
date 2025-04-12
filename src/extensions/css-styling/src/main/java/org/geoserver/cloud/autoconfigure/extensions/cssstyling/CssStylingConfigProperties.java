/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.cssstyling;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for CSS Styling extension.
 *
 * <p>
 * This class defines the configuration properties for the CSS styling extension,
 * with a prefix of "geoserver.extension.css-styling".
 *
 * <p>
 * Available properties:
 * <ul>
 *   <li><b>enabled</b>: Whether the CSS Styling extension is enabled (default: true)</li>
 * </ul>
 *
 * <p>
 * Example YAML configuration:
 * <pre>{@code
 * geoserver:
 *   extension:
 *     css-styling:
 *       enabled: true
 * }</pre>
 *
 * @since 2.27.0
 */
@ConfigurationProperties(prefix = "geoserver.extension.css-styling")
public @Data class CssStylingConfigProperties {

    /** Default value for the enabled property (true) */
    static final boolean DEFAULT = true;

    /**
     * Whether the CSS Styling extension is enabled.
     * When true, the CSS styling handler will be registered.
     * When false, the extension is disabled.
     */
    private boolean enabled = DEFAULT;
}
