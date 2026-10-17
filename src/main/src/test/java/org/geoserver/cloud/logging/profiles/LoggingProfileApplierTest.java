/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.logging.profiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.logging.LogLevel.DEBUG;
import static org.springframework.boot.logging.LogLevel.INFO;
import static org.springframework.boot.logging.LogLevel.OFF;
import static org.springframework.boot.logging.LogLevel.TRACE;
import static org.springframework.boot.logging.LogLevel.WARN;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;

/** Runs against the Logback logging system of the tests, on loggers under {@code test.profiles}. */
class LoggingProfileApplierTest {

    private static final String PARENT = "test.profiles.parent";
    private static final String CHILD = "test.profiles.parent.child";
    private static final String OTHER = "test.profiles.other";

    private LoggingSystem loggingSystem;
    private LogLevel rootLevel;
    private LoggingProfileApplier applier;

    @BeforeEach
    void setUp() {
        loggingSystem = LoggingSystem.get(getClass().getClassLoader());
        rootLevel = configuredLevel(LoggingSystem.ROOT_LOGGER_NAME);
        // the levels set by the externalized configuration at startup
        loggingSystem.setLogLevel(PARENT, WARN);
        loggingSystem.setLogLevel(CHILD, INFO);

        LoggingProfilesProperties config = new LoggingProfilesProperties();
        config.getProfiles().put("PARENT_DEBUG", Map.of(PARENT, DEBUG));
        config.getProfiles().put("OTHER_TRACE", Map.of(OTHER, TRACE));
        config.getProfiles().put("ROOT_OFF", Map.of("root", OFF));
        applier = new LoggingProfileApplier(loggingSystem, new LoggingProfiles(config));
    }

    @AfterEach
    void tearDown() {
        loggingSystem.setLogLevel(LoggingSystem.ROOT_LOGGER_NAME, rootLevel);
        loggingSystem.setLogLevel(PARENT, null);
        loggingSystem.setLogLevel(CHILD, null);
        loggingSystem.setLogLevel(OTHER, null);
    }

    @Test
    void setsProfileLevels() {
        applier.apply("PARENT_DEBUG");

        assertThat(configuredLevel(PARENT)).isEqualTo(DEBUG);
    }

    // The configured level of a descendant would otherwise hide the profile level
    @Test
    void clearsDescendantLevels() {
        applier.apply("PARENT_DEBUG");

        assertThat(configuredLevel(CHILD)).isNull();
        assertThat(effectiveLevel(CHILD)).isEqualTo(DEBUG);
    }

    @Test
    void restoresOnProfileChange() {
        applier.apply("PARENT_DEBUG");
        applier.apply("OTHER_TRACE");

        assertThat(configuredLevel(PARENT)).isEqualTo(WARN);
        assertThat(configuredLevel(CHILD)).isEqualTo(INFO);
        assertThat(configuredLevel(OTHER)).isEqualTo(TRACE);
    }

    @Test
    void restoresUnsetLevels() {
        applier.apply("OTHER_TRACE");
        applier.apply("DEFAULT_LOGGING");

        assertThat(configuredLevel(OTHER)).isNull();
    }

    @Test
    void defaultRestoresConfigured() {
        applier.apply("PARENT_DEBUG");
        applier.apply("DEFAULT_LOGGING");

        assertThat(configuredLevel(PARENT)).isEqualTo(WARN);
        assertThat(configuredLevel(CHILD)).isEqualTo(INFO);
    }

    @Test
    void unknownRestoresConfigured() {
        applier.apply("PARENT_DEBUG");
        applier.apply("TEST_LOGGING");

        assertThat(configuredLevel(PARENT)).isEqualTo(WARN);
        assertThat(configuredLevel(CHILD)).isEqualTo(INFO);
    }

    @Test
    void nullIsDefault() {
        applier.apply("PARENT_DEBUG");
        applier.apply(null);

        assertThat(configuredLevel(PARENT)).isEqualTo(WARN);
    }

    @Test
    void rootProfile() {
        applier.apply("ROOT_OFF");

        assertThat(configuredLevel(LoggingSystem.ROOT_LOGGER_NAME)).isEqualTo(OFF);
        assertThat(configuredLevel(PARENT)).isNull();
        assertThat(effectiveLevel(CHILD)).isEqualTo(OFF);

        applier.apply("DEFAULT_LOGGING");

        assertThat(configuredLevel(LoggingSystem.ROOT_LOGGER_NAME)).isEqualTo(rootLevel);
        assertThat(configuredLevel(PARENT)).isEqualTo(WARN);
        assertThat(configuredLevel(CHILD)).isEqualTo(INFO);
    }

    private LogLevel configuredLevel(String loggerName) {
        LoggerConfiguration logger = loggingSystem.getLoggerConfiguration(loggerName);
        return logger.getConfiguredLevel();
    }

    private LogLevel effectiveLevel(String loggerName) {
        LoggerConfiguration logger = loggingSystem.getLoggerConfiguration(loggerName);
        return logger.getEffectiveLevel();
    }
}
