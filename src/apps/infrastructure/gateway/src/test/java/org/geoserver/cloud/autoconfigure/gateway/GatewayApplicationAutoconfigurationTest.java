/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.cloud.gateway.filter.RouteProfileGatewayFilterFactory;
import org.geoserver.cloud.gateway.filter.StripBasePathGatewayFilterFactory;
import org.geoserver.cloud.gateway.predicate.RegExpQueryRoutePredicateFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;

class GatewayApplicationAutoconfigurationTest {

    private ReactiveWebApplicationContextRunner runner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GatewayApplicationAutoconfiguration.class));

    @Test
    void testDefaultAppContextContributions() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(RegExpQueryRoutePredicateFactory.class)
                .hasSingleBean(RouteProfileGatewayFilterFactory.class)
                .hasSingleBean(StripBasePathGatewayFilterFactory.class));
    }
}
