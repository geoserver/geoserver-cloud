/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.oidc;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServerWebUI;
import org.geoserver.configuration.extension.oidc.OidcCoreConfiguration;
import org.geoserver.configuration.extension.oidc.OidcWebUIConfiguration;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for the GeoServer OIDC (OpenID Connect / OAuth2 Login) extension.
 *
 * <p>The extension is enabled by default and can be disabled with:
 *
 * <pre>{@code
 * geoserver:
 *   extension:
 *     security:
 *       oidc:
 *         enabled: false
 * }</pre>
 *
 * <p>The extension makes the OIDC authentication provider available to configure from the GeoServer admin UI (Security
 * > Authentication). Per-provider settings (client id/secret, authorization/token/userinfo URIs) are stored in the
 * GeoServer data directory as the standard security filter config and are not managed by Spring Boot configuration
 * properties.
 *
 * @see OidcCoreConfiguration
 * @see OidcWebUIConfiguration
 * @since 3.0.3
 */
@AutoConfiguration
@EnableConfigurationProperties(OidcConfigProperties.class)
@Import({OidcAutoConfiguration.Enabled.class, OidcAutoConfiguration.WebUI.class})
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.extensions.security.oidc")
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class OidcAutoConfiguration {

    /** Core configuration: registers the OIDC authentication provider and supporting beans. */
    @ConditionalOnOidc
    @Import(OidcCoreConfiguration.class)
    static @Configuration class Enabled {
        public @PostConstruct void log() {
            log.info("OIDC extension enabled");
        }
    }

    /** Web UI configuration: registers the admin panel and login-button beans (webui service only). */
    @ConditionalOnOidc
    @ConditionalOnGeoServerWebUI
    @ConditionalOnClass(AuthenticationFilterPanelInfo.class)
    @Import(OidcWebUIConfiguration.class)
    static @Configuration class WebUI {}
}
