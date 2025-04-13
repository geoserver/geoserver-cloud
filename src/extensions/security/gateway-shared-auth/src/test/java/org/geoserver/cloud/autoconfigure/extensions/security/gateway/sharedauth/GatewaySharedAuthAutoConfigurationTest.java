/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.gateway.sharedauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.cloud.autoconfigure.extensions.security.gateway.sharedauth.ServerModeConfiguration.GatewaySharedAuthFilterPanelInfo;
import org.geoserver.cloud.security.gateway.sharedauth.GatewaySharedAuthenticationProvider;
import org.geoserver.config.GeoServer;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.web.GeoServerApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * Test suite for {@link GatewaySharedAuthAutoConfiguration}
 *
 * @since 2.27.0.0
 */
class GatewaySharedAuthAutoConfigurationTest {

    private WebApplicationContextRunner runner;

    @BeforeEach
    void setup() {
        // Create a mock GeoServer instance to satisfy @ConditionalOnGeoServer
        var mockGeoServer = mock(GeoServer.class);

        runner = new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(GatewaySharedAuthenticationAutoConfiguration.class))
                .withBean("geoServer", GeoServer.class, () -> mockGeoServer)
                .withBean(GeoServerSecurityManager.class, () -> mock(GeoServerSecurityManager.class));
    }

    @Test
    void testEnabledClientMode() {
        runner.withClassLoader(new FilteredClassLoader(GeoServerApplication.class))
                .withPropertyValues("geoserver.extension.security.gateway-shared-auth.enabled=true")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(GatewaySharedAuthenticationProvider.class)
                            .hasSingleBean(ClientModeConfiguration.class)
                            .doesNotHaveBean(ServerModeConfiguration.class)
                            .doesNotHaveBean(DisabledModeConfiguration.class)
                            .doesNotHaveBean(GatewaySharedAuthFilterPanelInfo.class);
                });
    }

    @Test
    void testEnabledServerMode() {
        runner.withPropertyValues(
                        "geoserver.extension.security.gateway-shared-auth.enabled=true",
                        "geoserver.service.webui.enabled=true")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(GatewaySharedAuthenticationProvider.class)
                            .hasSingleBean(ServerModeConfiguration.class)
                            .doesNotHaveBean(ClientModeConfiguration.class)
                            .doesNotHaveBean(DisabledModeConfiguration.class)
                            .hasSingleBean(GatewaySharedAuthFilterPanelInfo.class);
                });
    }

    @Test
    void testDisabled() {
        runner.withPropertyValues("geoserver.extension.security.gateway-shared-auth.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(GatewaySharedAuthenticationProvider.class);
                    assertThat(context).hasSingleBean(DisabledModeConfiguration.class);
                    assertThat(context).doesNotHaveBean(ClientModeConfiguration.class);
                    assertThat(context).doesNotHaveBean(ServerModeConfiguration.class);
                });
    }
}
