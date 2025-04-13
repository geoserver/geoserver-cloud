/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.autoconfigure.vectorformats;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties to control which GeoTools DataAccessFactory
 * implementations are enabled or disabled.
 *
 * <p>
 * Example:
 *
 * <pre>
 * geotools.data.filtering:
 *   enabled: true  # Master switch for filtering
 *   vector-formats:
 *     # Alphabetically sorted factory display names with proper escaping
 *     "[Application Schema DataAccess]": ${geoserver.extension.appschema.enabled:false}
 *     "[GeoPackage]": true
 *     "[Generalizing data store]": ${geoserver.extension.pregeneralized.enabled:false}
 *     "[Oracle NG]": false
 *     "[PostGIS]": true
 *     "[Shapefile]": true
 *     "[Web Feature Server (NG)]": ${my.wfs.connection.enabled:false}
 * </pre>
 *
 * <p>
 * This configuration allows for fine-grained control over which data store
 * factories are available within the application, using user-friendly display
 * names with placeholder resolution. Note that factory names containing special
 * characters should be properly escaped with the format "[Factory Name]".
 */
@ConfigurationProperties(value = DataAccessFactoryFilterConfigProperties.PREFIX)
@Data
public class DataAccessFactoryFilterConfigProperties {

    static final String PREFIX = "geotools.data.filtering";
    static final String ENABLED_PROP = PREFIX + ".enabled";

    /**
     * Whether the DataAccessFactory filtering system is enabled.
     */
    private boolean enabled = true;

    /**
     * Map of factory display names to their enabled status. Keys are the
     * user-friendly display names of DataAccessFactory implementations. Values are
     * boolean flags indicating whether the factory is enabled (true) or disabled
     * (false).
     */
    private Map<String, Boolean> vectorFormats = new HashMap<>();

    /**
     * Returns whether a factory is enabled based on its display name.
     *
     * @param displayName the user-friendly display name of the factory
     * @return true if the factory is explicitly enabled or not present in the
     *         configuration
     */
    public boolean isFactoryEnabled(String displayName) {
        return vectorFormats.getOrDefault(displayName, true);
    }
}
