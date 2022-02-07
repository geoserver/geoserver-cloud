/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.app;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Auto configuration to log basic application info at {@link ApplicationReadyEvent app startup}
 *
 * <p>Expects the following properties be present in the {@link Environment}: {@literal
 * spring.application.name}, {@literal info.instance-id}.
 *
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
public class StartupLoggerAutoConfiguration {

    @Bean
    StartupLogger appStartupLogger() {
        return new StartupLogger();
    }
}
