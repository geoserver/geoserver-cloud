/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gateway;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.security.gateway.sharedauth.GatewaySharedAuhenticationPostFilter;
import org.geoserver.cloud.security.gateway.sharedauth.GatewaySharedAuhenticationPreFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;

@AutoConfiguration
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
    GatewaySharedAuhenticationPreFilter gatewaySharedAuhenticationGlobalPreFilter() {
        return new GatewaySharedAuhenticationPreFilter();
    }

    @Bean
    GatewaySharedAuhenticationPostFilter gatewaySharedAuhenticationGlobalPostFilter() {
        return new GatewaySharedAuhenticationPostFilter();
    }
}
