/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.gateway;

import org.geoserver.cloud.gateway.filter.ClientAddressForwardedHeaderTransformer;
import org.geoserver.cloud.gateway.filter.TrustedProxies;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;

/**
 * Defines the {@code forwardedHeaderTransformer} bean of the gateway, a {@link ClientAddressForwardedHeaderTransformer},
 * when {@code server.forward-headers-strategy} is {@code framework}.
 *
 * <p>Spring Boot defines its own transformer under that name when no bean of the type is defined before it, and the
 * {@code WebHttpHandlerBuilder} finds the transformer by the name. This autoconfiguration runs before
 * {@link ReactiveWebServerFactoryAutoConfiguration}, which then backs off.
 */
@AutoConfiguration(before = ReactiveWebServerFactoryAutoConfiguration.class)
@ConditionalOnProperty(value = "server.forward-headers-strategy", havingValue = "framework")
public class ForwardedHeadersAutoConfiguration {

    /**
     * Applies the forwarded headers to the requests, with the client address read from the {@code X-Forwarded-For}
     * header when a trusted proxy sent the request.
     *
     * @see ClientAddressForwardedHeaderTransformer
     */
    @Bean
    ForwardedHeaderTransformer forwardedHeaderTransformer(TrustedProxies trustedProxies) {
        return new ClientAddressForwardedHeaderTransformer(trustedProxies);
    }
}
