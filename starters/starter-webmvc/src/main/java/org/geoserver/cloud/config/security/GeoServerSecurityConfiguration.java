/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.security;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

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
@ImportResource({"classpath*:/applicationSecurityContext.xml"})
@Slf4j
public class GeoServerSecurityConfiguration {

    public @PostConstruct void log() {
        log.info(
                "GeoServer security configured through classpath*:/applicationSecurityContext.xml");
    }
}
