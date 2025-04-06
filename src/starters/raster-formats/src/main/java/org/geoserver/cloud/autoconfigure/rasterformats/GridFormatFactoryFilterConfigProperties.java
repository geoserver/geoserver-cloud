/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.rasterformats;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties to control which GeoTools GridFormatFactorySpi
 * implementations are enabled or disabled.
 *
 * <p>Example:
 *
 * <pre>
 * geotools.data.filtering:
 *   enabled: true  # Master switch for filtering
 *   raster-formats:
 *     # Alphabetically sorted format display names with proper escaping
 *     "[ArcGrid]": true
 *     "[GeoTIFF]": true
 *     "[ImageMosaic]": true
 *     "[WorldImage]": true
 * </pre>
 *
 * <p>This configuration allows for fine-grained control over which raster format
 * factories are available within the application, using user-friendly display
 * names with placeholder resolution. Note that format names containing special
 * characters should be properly escaped with the format "[Format Name]".
 */
@ConfigurationProperties(value = GridFormatFactoryFilterConfigProperties.PREFIX)
@Data
public class GridFormatFactoryFilterConfigProperties {

    static final String PREFIX = "geotools.data.filtering";
    static final String ENABLED_PROP = PREFIX + ".enabled";

    /**
     * Whether the GridFormatFactorySpi filtering system is enabled.
     */
    private boolean enabled = true;

    /**
     * Map of format display names to their enabled status. Keys are the
     * user-friendly display names of GridFormatFactorySpi implementations. Values are
     * boolean flags indicating whether the factory is enabled (true) or disabled
     * (false).
     */
    private Map<String, Boolean> rasterFormats = new HashMap<>();

    /**
     * Returns whether a format is enabled based on its display name.
     *
     * @param displayName the user-friendly display name of the format
     * @return true if the format is explicitly enabled or not present in the
     *         configuration
     */
    public boolean isFormatEnabled(String displayName) {
        // Simple lookup by format name - Spring Boot property binding
        // handles the YAML syntax already
        Boolean configuredValue = rasterFormats.get(displayName);

        // Default to enabled if not configured
        boolean enabled = configuredValue != null ? configuredValue : true;

        // Log at debug level to avoid excessive logging
        if (configuredValue != null) {
            org.slf4j.LoggerFactory.getLogger(GridFormatFactoryFilterConfigProperties.class)
                    .debug("Format '{}' configured value: {}", displayName, enabled);
        } else {
            org.slf4j.LoggerFactory.getLogger(GridFormatFactoryFilterConfigProperties.class)
                    .debug("Format '{}' has no configuration, defaulting to enabled", displayName);
        }

        return enabled;
    }
}
