/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.security.gateway.sharedauth;

import static org.geoserver.cloud.security.gateway.sharedauth.GatewaySharedAuthenticationProvider.Mode.CLIENT;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.security.gateway.sharedauth.GatewaySharedAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the Gateway Shared Authentication system in client mode.
 *
 * <p>This configuration is automatically activated when running in any service other than the WebUI.
 * It creates a {@link GatewaySharedAuthenticationProvider} in CLIENT mode, which is designed for all
 * services except WebUI.</p>
 *
 * <p>In client mode, the authentication filter reads authentication headers from incoming requests
 * that have been forwarded by the API Gateway. These headers contain the authenticated username and roles
 * that were originally set by the WebUI service when the user logged in.</p>
 *
 * <p>Note: Previously, this was controlled by the configuration property
 * {@code geoserver.extension.security.gateway-shared-auth.server=false}, but now the mode
 * is automatically determined based on the application type.
 *
 * @see GatewaySharedAuthenticationFilter.ClientFilter
 * @see ServerModeConfiguration
 * @see DisabledModeConfiguration
 * @since 1.9
 */
@Configuration
@ConditionalOnClientMode
@Slf4j
class ClientModeConfiguration {
    @PostConstruct
    void log() {
        log.info("gateway-shared-auth enabled in client mode");
    }

    @Bean
    GatewaySharedAuthenticationProvider gatewaySharedAuthenticationProvider() {
        return new GatewaySharedAuthenticationProvider(CLIENT);
    }
}
