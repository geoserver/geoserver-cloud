/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.configuration.core.main;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.spring.config.annotations.TranspileXmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Loads the {@link GeoServerMainSecurityConfiguration_Generated transpiled} geoserver security bean definitions from
 * {@code jar:gs-main-.*!/applicationSecurityContext.xml}.
 *
 * <p>Note this config <strong>does not</strong> provide a {@link GeoServerSecurityManager}, but the
 * {@code GeoServerMainSecurityAutoConfiguration} will.
 */
@Configuration(proxyBeanMethods = false)
@TranspileXmlConfig(
        locations = "jar:gs-main-.*!/applicationSecurityContext.xml",
        excludes = {"authenticationManager", "disabledFilter"})
@Import(GeoServerMainSecurityConfiguration_Generated.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.security")
public class GeoServerMainSecurityConfiguration {

    @PostConstruct
    void log() {
        log.info("GeoServer main security configuration loaded");
    }
}
