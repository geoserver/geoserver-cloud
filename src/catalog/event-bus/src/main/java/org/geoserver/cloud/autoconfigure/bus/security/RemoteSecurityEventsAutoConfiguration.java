/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.bus.security;

import org.geoserver.cloud.autoconfigure.bus.ConditionalOnGeoServerRemoteEventsEnabled;
import org.geoserver.cloud.bus.security.RemoteSecurityConfigEvent;
import org.geoserver.cloud.bus.security.RemoteSecurityEventBridge;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Loads geoserver security bean definitions from {@code
 * classpath*:/applicationSecurityContext.xml}.
 *
 * <p>Note that if spring boot auto-configuration is enabled, at the very least {@link
 * SecurityAutoConfiguration} or {@link UserDetailsServiceAutoConfiguration} must be disabled, for
 * example using the following annotation in a {@link Configuration @Configuration} class:
 *
 * <pre>{@code
 * &#64;EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
 * }</pre>
 *
 * <p>The {@code geoserver.security.enabled=false} config property can be used as a flag to disable
 * this configuration. Defaults to {@code true}.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnGeoServerRemoteEventsEnabled
@AutoConfigureAfter(BusAutoConfiguration.class)
@RemoteApplicationEventScan(basePackageClasses = {RemoteSecurityConfigEvent.class})
public class RemoteSecurityEventsAutoConfiguration {

    public @Bean RemoteSecurityEventBridge remoteSecurityEventBridge() {
        return new RemoteSecurityEventBridge();
    }
}
