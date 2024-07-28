/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.cloud.security.gateway.sharedauth.GatewaySharedAuhenticationPostFilter;
import org.geoserver.cloud.security.gateway.sharedauth.GatewaySharedAuhenticationPreFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;

class GatewaySharedAuthAutoConfigurationTest {

    private ReactiveWebApplicationContextRunner runner =
            new ReactiveWebApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(GatewaySharedAuthAutoConfiguration.class));

    @Test
    void enabledByDefault() {
        assertEnabled(runner);
    }

    @Test
    void enabledByConfig() {
        assertEnabled(
                runner.withPropertyValues("geoserver.security.gateway-shared-auth.enabled: true"));
    }

    private void assertEnabled(ReactiveWebApplicationContextRunner contextRunner) {
        contextRunner.run(
                context ->
                        assertThat(context)
                                .hasNotFailed()
                                .hasSingleBean(GatewaySharedAuhenticationPreFilter.class)
                                .hasSingleBean(GatewaySharedAuhenticationPostFilter.class));
    }

    @Test
    void disableByConfig() {
        runner.withPropertyValues("geoserver.security.gateway-shared-auth.enabled: false")
                .run(
                        context ->
                                assertThat(context)
                                        .hasNotFailed()
                                        .doesNotHaveBean(GatewaySharedAuhenticationPreFilter.class)
                                        .doesNotHaveBean(
                                                GatewaySharedAuhenticationPostFilter.class));
    }
}
