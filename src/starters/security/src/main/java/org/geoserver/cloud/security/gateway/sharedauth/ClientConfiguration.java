/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.security.gateway.sharedauth;

import static org.geoserver.cloud.security.gateway.sharedauth.GatewaySharedAuthenticationProvider.Mode.CLIENT;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contributes a {@link GatewaySharedAuthenticationProvider} in client mode.
 *
 * @see ServerConfiguration
 * @since 1.9
 */
@Configuration
public class ClientConfiguration {

    @Bean
    GatewaySharedAuthenticationProvider gatewaySharedAuthenticationProvider() {
        return new GatewaySharedAuthenticationProvider(CLIENT);
    }
}
