/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.cloud.gateway.proxy.LenientContentTypeProxyExchange;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.cloud.gateway.server.mvc.handler.ProxyExchange;
import org.springframework.web.client.RestClient;

class GatewayProxyExchangeAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GatewayProxyExchangeAutoConfiguration.class))
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withBean(GatewayMvcProperties.class);

    @Test
    void registersLenientProxyExchange() {
        runner.run(context -> assertThat(context).hasNotFailed().hasSingleBean(LenientContentTypeProxyExchange.class));
    }

    @Test
    void honorsGatewayEnabledProperty() {
        runner.withPropertyValues("spring.cloud.gateway.server.webmvc.enabled=false")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(ProxyExchange.class));
    }
}
