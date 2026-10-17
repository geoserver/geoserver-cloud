/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.logging.profiles;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.logging.LogLevel;

/**
 * Logging profiles defined in the externalized configuration.
 *
 * <pre>{@code
 * geoserver:
 *   logging:
 *     profiles:
 *       CATALOG_LOGGING:
 *         org.geoserver.catalog: debug
 *         org.geoserver.cloud.catalog: debug
 * }</pre>
 *
 * @see LoggingProfiles
 */
@Data
@ConfigurationProperties(prefix = "geoserver.logging")
public class LoggingProfilesProperties {

    /** Logging profiles by name. Each profile maps logger names to levels. */
    private Map<String, Map<String, LogLevel>> profiles = new LinkedHashMap<>();
}
