/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.environmentadmin;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.security.GeoServerMainSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the Environment Admin Authentication extension.
 *
 * <p>
 * This auto-configuration class enables the Environment Admin Authentication extension in GeoServer Cloud,
 * allowing administrators to set the admin username and password through environment variables or
 * configuration properties. It will be activated when the following conditions are met:
 * <ul>
 *   <li>GeoServer security is enabled (the default)</li>
 *   <li>The geoserver.extension.security.environment-admin.enabled property is true (the default)</li>
 * </ul>
 *
 * <p>
 * This extension is particularly useful in containerized environments where you want to set admin credentials
 * without modifying the security configuration XML files.
 *
 * @since 2.27.0
 */
// run before GeoServerMainSecurityAutoConfiguration so the provider is available when
// GeoServerSecurityManager calls GeoServerExtensions.extensions(GeoServerSecurityProvider.class)
@AutoConfiguration(before = GeoServerMainSecurityAutoConfiguration.class)
@ConditionalOnEnvironmentAdmin
@EnableConfigurationProperties(EnvironmentAdminConfigProperties.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.security.environmentadmin")
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class EnvironmentAdminAutoConfiguration {

    @PostConstruct
    void log() {
        log.info("Environment Admin Authentication extension enabled");
    }

    /**
     * Creates the EnvironmentAdminAuthenticationProvider bean.
     *
     * <p>
     * This provider allows administrators to set the admin username and password through environment variables
     * or configuration properties, bypassing the need to modify security configuration XML files.
     *
     * @return the EnvironmentAdminAuthenticationProvider instance
     */
    @Bean
    EnvironmentAdminAuthenticationProvider environmentAdminAuthenticationProvider() {
        return new EnvironmentAdminAuthenticationProvider();
    }
}
