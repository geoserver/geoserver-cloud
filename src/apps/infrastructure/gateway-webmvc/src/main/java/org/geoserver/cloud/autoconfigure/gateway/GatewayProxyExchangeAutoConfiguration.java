/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gateway;

import org.geoserver.cloud.gateway.proxy.LenientContentTypeProxyExchange;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.server.mvc.GatewayServerMvcAutoConfiguration;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * Registers {@link LenientContentTypeProxyExchange} as the gateway's {@code ProxyExchange}. Runs before
 * {@link GatewayServerMvcAutoConfiguration}, because its default {@code RestClientProxyExchange} is only created when
 * no other {@code ProxyExchange} bean exists.
 *
 * @since 3.1.0
 */
@AutoConfiguration(before = GatewayServerMvcAutoConfiguration.class)
@ConditionalOnProperty(name = GatewayMvcProperties.PREFIX + ".enabled", matchIfMissing = true)
public class GatewayProxyExchangeAutoConfiguration {

    @Bean
    LenientContentTypeProxyExchange lenientContentTypeProxyExchange(
            RestClient.Builder restClientBuilder, GatewayMvcProperties properties) {
        return new LenientContentTypeProxyExchange(restClientBuilder.build(), properties);
    }
}
