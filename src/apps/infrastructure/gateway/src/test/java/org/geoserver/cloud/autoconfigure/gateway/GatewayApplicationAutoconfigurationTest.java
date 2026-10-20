/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.cloud.gateway.filter.ClientAddressForwardedHeaderTransformer;
import org.geoserver.cloud.gateway.filter.RouteProfileGatewayFilterFactory;
import org.geoserver.cloud.gateway.filter.StripBasePathGatewayFilterFactory;
import org.geoserver.cloud.gateway.filter.TrustedProxies;
import org.geoserver.cloud.gateway.predicate.RegExpQueryRoutePredicateFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;

class GatewayApplicationAutoconfigurationTest {

    private ReactiveWebApplicationContextRunner runner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GatewayApplicationAutoconfiguration.class));

    @Test
    void testDefaultAppContextContributions() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(RegExpQueryRoutePredicateFactory.class)
                .hasSingleBean(RouteProfileGatewayFilterFactory.class)
                .hasSingleBean(StripBasePathGatewayFilterFactory.class)
                .hasSingleBean(TrustedProxies.class));
    }

    @Test
    void trustedProxies_noneByDefault() {
        runner.run(context -> assertThat(context.getBean(TrustedProxies.class).isTrusted("127.0.0.1"))
                .isFalse());
    }

    @Test
    void trustedProxies_fromProperty() {
        runner.withPropertyValues("geoserver.gateway.trusted-proxies=10\\.0\\.0\\.\\d+")
                .run(context -> {
                    TrustedProxies trustedProxies = context.getBean(TrustedProxies.class);
                    assertThat(trustedProxies.isTrusted("10.0.0.5")).isTrue();
                    assertThat(trustedProxies.isTrusted("192.168.1.10")).isFalse();
                });
    }

    // the bean name is the one the WebHttpHandlerBuilder looks up
    @Test
    void forwardedHeaderTransformer_framework() {
        runner.withConfiguration(AutoConfigurations.of(ForwardedHeadersAutoConfiguration.class))
                .withPropertyValues("server.forward-headers-strategy=framework")
                .run(context -> {
                    assertThat(context).hasBean("forwardedHeaderTransformer");
                    assertThat(context.getBean(ForwardedHeaderTransformer.class))
                            .isInstanceOf(ClientAddressForwardedHeaderTransformer.class);
                });
    }

    @Test
    void forwardedHeaderTransformer_otherStrategy() {
        runner.withConfiguration(AutoConfigurations.of(ForwardedHeadersAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(ForwardedHeaderTransformer.class));
    }

    // Spring Boot defines its transformer unless one is defined before it in the autoconfiguration order
    @Test
    void forwardedHeaderTransformer_replacesSpringBoots() {
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        ReactiveWebServerFactoryAutoConfiguration.class,
                        ForwardedHeadersAutoConfiguration.class,
                        GatewayApplicationAutoconfiguration.class))
                .withPropertyValues("server.forward-headers-strategy=framework")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(ForwardedHeaderTransformer.class))
                            .isInstanceOf(ClientAddressForwardedHeaderTransformer.class);
                });
    }
}
