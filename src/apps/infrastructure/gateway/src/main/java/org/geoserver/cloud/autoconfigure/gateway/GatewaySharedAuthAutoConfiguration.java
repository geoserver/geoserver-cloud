/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gateway;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.security.gateway.sharedauth.GatewaySharedAuthenticationPostFilter;
import org.geoserver.cloud.security.gateway.sharedauth.GatewaySharedAuthenticationPreFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnProperty(
        name = "geoserver.security.gateway-shared-auth.enabled",
        havingValue = "true",
        matchIfMissing = true)
@Slf4j
public class GatewaySharedAuthAutoConfiguration {

    @PostConstruct
    void logEnabled() {
        log.info("gateway-shared-auth is enabled");
    }

    @Bean
    GatewaySharedAuthenticationPreFilter gatewaySharedAuthenticationGlobalPreFilter() {
        return new GatewaySharedAuthenticationPreFilter();
    }

    @Bean
    GatewaySharedAuthenticationPostFilter gatewaySharedAuthenticationGlobalPostFilter() {
        return new GatewaySharedAuthenticationPostFilter();
    }
}
