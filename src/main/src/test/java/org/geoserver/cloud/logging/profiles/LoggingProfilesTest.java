/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.logging.profiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.logging.LogLevel.DEBUG;
import static org.springframework.boot.logging.LogLevel.INFO;

import java.util.Map;
import org.junit.jupiter.api.Test;

class LoggingProfilesTest {

    private final LoggingProfilesProperties config = new LoggingProfilesProperties();

    @Test
    void defaultProfile() {
        LoggingProfiles profiles = new LoggingProfiles(config);

        assertThat(profiles.names()).containsExactly("DEFAULT_LOGGING");
        assertThat(profiles.levels("DEFAULT_LOGGING")).contains(Map.of());
    }

    @Test
    void unknownProfile() {
        LoggingProfiles profiles = new LoggingProfiles(config);

        assertThat(profiles.levels("TEST_LOGGING")).isEmpty();
        assertThat(profiles.levels(null)).isEmpty();
    }

    @Test
    void configuredProfile() {
        config.getProfiles().put("CATALOG_LOGGING", Map.of("org.geoserver.catalog", DEBUG, "root", INFO));

        LoggingProfiles profiles = new LoggingProfiles(config);

        assertThat(profiles.names()).containsExactly("DEFAULT_LOGGING", "CATALOG_LOGGING");
        assertThat(profiles.levels("CATALOG_LOGGING")).contains(Map.of("org.geoserver.catalog", DEBUG, "ROOT", INFO));
    }

    @Test
    void configurationOrder() {
        config.getProfiles().put("VERBOSE_LOGGING", Map.of("org.geoserver", DEBUG));
        config.getProfiles().put("DEBUG_CATALOG", Map.of("org.geoserver.catalog", DEBUG));

        LoggingProfiles profiles = new LoggingProfiles(config);

        assertThat(profiles.names()).containsExactly("DEFAULT_LOGGING", "VERBOSE_LOGGING", "DEBUG_CATALOG");
    }
}
