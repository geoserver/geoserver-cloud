/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.security.gateway.sharedauth;

import static org.geoserver.cloud.security.gateway.sharedauth.GatewaySharedAuthenticationProvider.Mode.DISABLED;

import org.geoserver.security.filter.AbstractFilterProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contributes a {@link GatewaySharedAuthenticationProvider} in disabled mode, essentially a no-op
 * {@link AbstractFilterProvider} to avoid starup failure when the gateway shared auth was enabled,
 * then disabled, and geoserver restarted.
 *
 * @see ClientConfiguration
 * @see ServerConfiguration
 * @since 1.9
 */
@Configuration
public class DisabledConfiguration {

    @Bean
    GatewaySharedAuthenticationProvider gatewaySharedAuthenticationProvider() {
        return new GatewaySharedAuthenticationProvider(DISABLED);
    }
}
