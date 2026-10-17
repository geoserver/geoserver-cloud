/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.logging.profiles;

import org.geoserver.cloud.event.config.LoggingInfoModified;
import org.geoserver.cloud.logging.profiles.LoggingProfileApplier;
import org.geoserver.cloud.logging.profiles.LoggingProfileListener;
import org.geoserver.cloud.logging.profiles.LoggingProfiles;
import org.geoserver.cloud.logging.profiles.LoggingProfilesProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Applies the logging profile selected in the GeoServer configuration to the loggers of the service, at startup and
 * each time an administrator selects another one, on this or another service instance.
 *
 * <p>The profiles replace the logging configuration files of vanilla GeoServer, ignored by GeoServer Cloud because
 * Spring Boot controls the logging.
 *
 * @see LoggingProfilesProperties
 */
@AutoConfiguration
@ConditionalOnBean(LoggingSystem.class)
@EnableConfigurationProperties(LoggingProfilesProperties.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class LoggingProfilesAutoConfiguration {

    @Bean
    LoggingProfiles loggingProfiles(LoggingProfilesProperties config) {
        return new LoggingProfiles(config);
    }

    @Bean
    LoggingProfileApplier loggingProfileApplier(LoggingSystem loggingSystem, LoggingProfiles profiles) {
        return new LoggingProfileApplier(loggingSystem, profiles);
    }

    /**
     * Applies the profile when GeoServer loads its configuration, and on the configuration change events, if the
     * optional events module is on the classpath
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(LoggingInfoModified.class)
    static class LoggingProfileListenerConfiguration {

        @Bean
        LoggingProfileListener loggingProfileListener(LoggingProfileApplier applier) {
            return new LoggingProfileListener(applier);
        }
    }
}
