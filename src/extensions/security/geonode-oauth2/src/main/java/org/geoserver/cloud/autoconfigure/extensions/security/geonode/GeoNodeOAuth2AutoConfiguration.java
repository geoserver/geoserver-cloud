/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.geonode;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.configuration.community.security.geonode.GeoNodeOAuth2Configuration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for GeoNode OAuth2 authentication extension.
 *
 * <p>
 * This auto-configuration class enables the GeoNode OAuth2 authentication extension in GeoServer Cloud,
 * allowing users to authenticate using GeoNode as an OAuth2 provider. It will be activated
 * when the following conditions are met:
 * <ul>
 *   <li>The geoserver.extension.security.geonode-oauth2.enabled property is true (the default)</li>
 * </ul>
 *
 * @since 2.27.0
 * @see GeoNodeOAuth2Configuration
 */
@AutoConfiguration
@ConditionalOnGeoNodeOAuth2
@EnableConfigurationProperties(GeoNodeOAuth2ConfigProperties.class)
@Import(GeoNodeOAuth2Configuration.class)
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.security.geonode")
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class GeoNodeOAuth2AutoConfiguration {

    /**
     * Logs a message indicating that the GeoNode OAuth2 extension is enabled.
     *
     * <p>
     * This method is called automatically after the bean is constructed and its dependencies
     * are injected. It provides a clear indication in the logs that the GeoNode OAuth2
     * authentication extension has been successfully enabled and loaded.
     *
     * <p>
     * The log message appears at INFO level and can be useful for troubleshooting
     * and verifying the extension's activation status during application startup.
     */
    @PostConstruct
    void log() {
        log.info("GeoNode OAuth2 authentication extension enabled");
    }
}
