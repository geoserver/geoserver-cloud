/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gateway;

import org.geoserver.cloud.autoconfigure.extensions.security.gatewaybearer.GatewayBearerAuthAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/** Keeps the gateway's existing public route behavior when no other security chain is configured. */
@AutoConfiguration(
        after = GatewayBearerAuthAutoConfiguration.class,
        beforeName = "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration")
public class GatewaySecurityFallbackConfiguration {

    @Bean
    @Order(2)
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain gatewayFallbackSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }
}
