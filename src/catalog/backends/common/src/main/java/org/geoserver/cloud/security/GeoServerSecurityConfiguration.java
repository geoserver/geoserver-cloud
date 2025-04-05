/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.security;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.cloud.autoconfigure.security.ConditionalOnGeoServerSecurityEnabled;
import org.geoserver.cloud.config.factory.ImportFilteredResource;
import org.geoserver.cloud.event.security.SecurityConfigChanged;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.security.CloudGeoServerSecurityFilterChainProxy;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;

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
@Configuration
@ImportFilteredResource(
        // exclude authenticationManager from applicationSecurityContext.xml
        GeoServerSecurityConfiguration.APPLICATION_SECURITY_CONTEXT_FILTER)
@Slf4j(topic = "org.geoserver.cloud.config.security")
@ConditionalOnGeoServerSecurityEnabled
public class GeoServerSecurityConfiguration {

    /** */
    public static final String APPLICATION_SECURITY_CONTEXT_FILTER =
            "classpath*:/applicationSecurityContext.xml#name=^(?!authenticationManager|filterChainProxy).*$";

    public @PostConstruct void log() {
        log.info("GeoServer security being configured through classpath*:/applicationSecurityContext.xml");
    }

    @Bean
    EnvironmentAdminAuthenticationProvider environmentAdminAuthenticationProvider() {
        return new EnvironmentAdminAuthenticationProvider();
    }

    @Bean
    GeoServerSecurityFilterChainProxy filterChainProxy(GeoServerSecurityManager sm) {
        return new CloudGeoServerSecurityFilterChainProxy(sm);
    }

    /**
     * Override the {@code authenticationManager} bean defined in {@code gs-main}'s {@code
     * applicationSecurityContext.xml} with a version that notifies other services of any security
     * configuration change, and listens to remote events from other services in order to {@link
     * GeoServerSecurityManager#reload() reload} the config.
     * @param lock
     *
     * @return {@link CloudGeoServerSecurityManager}
     */
    @Bean(name = {"authenticationManager", "geoServerSecurityManager"})
    @DependsOn({"extensions"})
    CloudGeoServerSecurityManager cloudAuthenticationManager( //
            @Lazy GeoServerConfigurationLock lock,
            GeoServerDataDirectory dataDir, //
            ApplicationEventPublisher localContextPublisher, //
            UpdateSequence updateSequence, //
            EnvironmentAdminAuthenticationProvider envAuth //
            ) throws Exception {

        Consumer<SecurityConfigChanged> publisher = localContextPublisher::publishEvent;
        Supplier<Long> updateSequenceIncrementor = updateSequence::nextValue;

        return new CloudGeoServerSecurityManager(lock, dataDir, publisher, updateSequenceIncrementor, List.of(envAuth));
    }
}
