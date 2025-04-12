/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.gateway.sharedauth;

import static org.geoserver.cloud.security.gateway.sharedauth.GatewaySharedAuthenticationProvider.Mode.DISABLED;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.security.gateway.sharedauth.GatewaySharedAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the Gateway Shared Authentication system when it's explicitly disabled.
 *
 * <p>This configuration is active when {@code geoserver.extension.security.gateway-shared-auth.enabled=false}.
 * It creates a {@link GatewaySharedAuthenticationProvider} in DISABLED mode, which uses a no-op filter
 * that simply passes requests through without processing.</p>
 *
 * <p>Having this disabled configuration is important for backward compatibility when the filter
 * has been previously enabled and then disabled. It prevents startup failures and WebUI security
 * settings editing failures by providing a valid but inactive filter implementation.</p>
 *
 * @see GatewaySharedAuthenticationFilter.DisabledFilter
 * @see ServerModeConfiguration
 * @see ClientModeConfiguration
 * @since 1.9
 */
@Configuration
@ConditionalOnGatewaySharedAuthDisabled
@Slf4j
class DisabledModeConfiguration {

    @PostConstruct
    void log() {
        log.info("gateway-shared-auth disabled");
    }

    @Bean
    GatewaySharedAuthenticationProvider gatewaySharedAuthenticationProvider() {
        return new GatewaySharedAuthenticationProvider(DISABLED);
    }
}
