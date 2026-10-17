/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.logging.profiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.event.config.LoggingInfoModified;
import org.geoserver.cloud.event.config.LoggingInfoSet;
import org.geoserver.cloud.logging.profiles.LoggingProfileListener;
import org.geoserver.cloud.logging.profiles.LoggingProfiles;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.impl.LoggingInfoImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class LoggingProfilesAutoConfigurationTest {

    private static final String LOGGER = "test.autoconfig.logger";

    private final LoggingSystem loggingSystem = LoggingSystem.get(getClass().getClassLoader());

    private final LoggingInfo logging = new LoggingInfoImpl();

    private final GeoServer geoServer = mock(GeoServer.class);

    private ApplicationContextRunner runner;

    @BeforeEach
    void setUp() {
        when(geoServer.getLogging()).thenReturn(logging);
        runner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(LoggingProfilesAutoConfiguration.class))
                .withBean(LoggingSystem.class, () -> loggingSystem)
                .withPropertyValues("geoserver.logging.profiles.TEST_PROFILE.test.autoconfig.logger=debug");
    }

    @AfterEach
    void tearDown() {
        loggingSystem.setLogLevel(LOGGER, null);
    }

    @Test
    void bindsConfiguredProfiles() {
        runner.run(context -> {
            LoggingProfiles profiles = context.getBean(LoggingProfiles.class);
            assertThat(profiles.levels("TEST_PROFILE")).contains(Map.of(LOGGER, LogLevel.DEBUG));
            assertThat(profiles.names()).containsExactly("DEFAULT_LOGGING", "TEST_PROFILE");
        });
    }

    // GeoServer looks up its initializers by type, and runs them once it has loaded its configuration
    @Test
    void appliesAtStartup() {
        logging.setLevel("TEST_PROFILE");
        runner.run(context -> {
            GeoServerInitializer initializer = context.getBean(GeoServerInitializer.class);
            initializer.initialize(geoServer);
            assertThat(configuredLevel()).isEqualTo(LogLevel.DEBUG);
        });
    }

    @Test
    void appliesLoggingInfoSet() {
        logging.setLevel("TEST_PROFILE");
        runner.run(context -> {
            context.publishEvent(LoggingInfoSet.createLocal(1L, logging));
            assertThat(configuredLevel()).isEqualTo(LogLevel.DEBUG);
        });
    }

    @Test
    void appliesRemoteChange() {
        runner.run(context -> {
            Patch patch = new Patch().with("level", "TEST_PROFILE");
            LoggingInfoModified event = LoggingInfoModified.createLocal(1L, logging, patch);
            event.setRemote(true);
            context.publishEvent(event);
            assertThat(configuredLevel()).isEqualTo(LogLevel.DEBUG);
        });
    }

    @Test
    void ignoresOtherChanges() {
        runner.run(context -> {
            Patch patch = new Patch().with("stdOutLogging", true);
            context.publishEvent(LoggingInfoModified.createLocal(1L, logging, patch));
            assertThat(configuredLevel()).isNull();
        });
    }

    @Test
    void noListenerWithoutEvents() {
        runner.withClassLoader(new FilteredClassLoader(LoggingInfoModified.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(LoggingProfiles.class);
                    assertThat(context).doesNotHaveBean(LoggingProfileListener.class);
                });
    }

    @Test
    void noBeansWithoutLoggingSystem() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(LoggingProfilesAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(LoggingProfiles.class));
    }

    private LogLevel configuredLevel() {
        return loggingSystem.getLoggerConfiguration(LOGGER).getConfiguredLevel();
    }
}
