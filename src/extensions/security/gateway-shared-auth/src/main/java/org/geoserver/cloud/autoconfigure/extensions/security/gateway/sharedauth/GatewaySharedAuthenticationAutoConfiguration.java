/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.security.gateway.sharedauth;

import java.util.Optional;
import org.geoserver.cloud.autoconfigure.security.GeoServerSecurityAutoConfiguration;
import org.geoserver.cloud.security.gateway.sharedauth.GatewaySharedAuthenticationInitializer;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * {@link AutoConfiguration @AutoConfiguration} to enable sharing the webui
 * form-based authentication object with the other services.
 *
 * <p>
 * When a user is logged in through the regular web ui's authentication form,
 * the {@link Authentication} object is held in the web ui's {@link Session}.
 * Hence, further requests to stateless services, as they're on separate
 * containers, don't share the webui session, and hence are executed as
 * anonymous.
 *
 * <p>
 * This {@link AutoConfiguration} enables a mechanism by which the authenticated
 * user name and roles can be shared with the stateless services through request
 * and response headrers, using the geoserver cloud gateway as the man in the
 * middle.
 *
 * <p>
 * The webui container will send a couple response headers with the
 * authenticated user name and roles. The gateway will store them in its own
 * session, and forward them to all services as request headers. The stateless
 * services will intercept these request headers and impersonate the
 * authenticated user as a {@link PreAuthenticatedAuthenticationToken}.
 *
 * <p>
 * At the same time, the gateway will take care of removing the webui response
 * headers from the responses sent to the clients, and from incoming client
 * requests.
 *
 * @see ServerConfiguration
 * @see ClientConfiguration
 * @since 1.9
 */
// run before GeoServerSecurityAutoConfiguration so the provider is available when
// GeoServerSecurityManager calls GeoServerExtensions.extensions(GeoServerSecurityProvider.class)
@AutoConfiguration(before = GeoServerSecurityAutoConfiguration.class)
@EnableConfigurationProperties(GatewaySharedAuthConfigProperties.class)
@Import({ServerModeConfiguration.class, ClientModeConfiguration.class, DisabledModeConfiguration.class})
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
public class GatewaySharedAuthenticationAutoConfiguration {

    @Bean
    ModuleStatusImpl gatewaySharedAuthModuleInfo(
            GatewaySharedAuthConfigProperties config, Optional<BuildProperties> buildProperties) {
        ModuleStatusImpl m = new ModuleStatusImpl();
        String version = buildProperties.map(BuildProperties::getVersion).orElse("UNKNOWN");
        m.setAvailable(true);
        m.setEnabled(config.isEnabled());
        m.setVersion(version);
        m.setName("GeoServer Cloud gateway shared authentication");
        m.setModule("gs-cloud-starter-security");
        m.setComponent("GatewaySharedAuthenticationFilter");
        m.setMessage(
                """
                The GatewaySharedAuthenticationFilter, specific to GeoServer Cloud,
                implements a mechanism in collaboration with the GeoServer Cloud
                Gateway application, for the microservices to share the authentication
                username and roles obtained when the user logs in through the WebUI.

                When enabled both in the gateway and the GeoServer microservices, and
                logged in through the WebUI, calls to other services (for example, in
                the Layer Preview page), will be performed with the same user as in
                the WebUI.
                """);
        m.setDocumentation("documentation");
        return m;
    }

    @Bean
    @ConditionalOnGatewaySharedAuthEnabled
    @ConditionalOnProperty(
            name = GatewaySharedAuthConfigProperties.AUTO_PROP,
            havingValue = "true",
            matchIfMissing = false)
    GatewaySharedAuthenticationInitializer gatewaySharedAuthenticationInitializer(GeoServerSecurityManager secManager) {
        return new GatewaySharedAuthenticationInitializer(secManager);
    }
}
