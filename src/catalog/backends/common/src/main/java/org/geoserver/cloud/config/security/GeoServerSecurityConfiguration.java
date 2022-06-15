/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.security;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.autoconfigure.security.ConditionalOnGeoServerSecurityEnabled;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.cloud.security.CloudGeoServerSecurityManager;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.ImportResource;

import javax.annotation.PostConstruct;

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
@ImportResource(
        reader = FilteringXmlBeanDefinitionReader.class, //
        // exclude authenticationManager from applicationSecurityContext.xml
        locations = {
            "classpath*:/applicationSecurityContext.xml#name=^(?!authenticationManager).*$"
        })
@Slf4j(topic = "org.geoserver.cloud.config.security")
@ConditionalOnGeoServerSecurityEnabled
public class GeoServerSecurityConfiguration {

    private @Value("${geoserver.security.enabled:#{null}}") Boolean enabled;

    public @PostConstruct void log() {
        log.info(
                "GeoServer security being configured through classpath*:/applicationSecurityContext.xml");
    }

    /**
     * Override the {@code authenticationManager} bean defined in {@code gs-main}'s {@code
     * applicationSecurityContext.xml} with a version that notifies other services of any security
     * configuration change, and listens to remote events from other services in order to {@link
     * GeoServerSecurityManager#reload() reload} the config.
     *
     * @return {@link CloudGeoServerSecurityManager}
     */
    @Bean(name = {"authenticationManager", "geoServerSecurityManager"})
    @DependsOn({"extensions"})
    public CloudGeoServerSecurityManager cloudAuthenticationManager(GeoServerDataDirectory dataDir)
            throws Exception {
        return new CloudGeoServerSecurityManager(dataDir);
    }
}
