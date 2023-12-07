/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.gwc.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheContextRunner;
import org.geoserver.cloud.autoconfigure.gwc.integration.WMSIntegrationAutoConfiguration.ForwardGetMapToGwcAspect;
import org.geoserver.gwc.web.GWCSettingsPage;
import org.geoserver.gwc.wms.CachingExtendedCapabilitiesProvider;
import org.geoserver.web.HeaderContribution;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.capabilities.GetCapabilitiesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.io.File;

/**
 * Test suite for {@link WMSIntegrationAutoConfiguration}
 *
 * @since 1.0
 */
class WMSIntegrationAutoConfigurationTest {

    @TempDir File tmpDir;
    WebApplicationContextRunner runner;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception {
        runner =
                GeoWebCacheContextRunner.newMinimalGeoWebCacheContextRunner(tmpDir)
                        .withConfiguration(
                                AutoConfigurations.of(WMSIntegrationAutoConfiguration.class));
    }

    /**
     * Set up mocked up minimum WMS beans that will satisfy the {@code @ConditionalOnBean(name =
     * {"wmsServiceTarget", "wms_1_1_1_GetCapabilitiesResponse"})} conditions on {@link
     * WMSIntegrationAutoConfiguration}
     */
    private WebApplicationContextRunner withMockWMS() {
        DefaultWebMapService mockWms = mock(DefaultWebMapService.class);
        org.geoserver.wms.capabilities.GetCapabilitiesResponse mockGetCapabilities =
                mock(GetCapabilitiesResponse.class);

        return runner.withBean("wmsServiceTarget", DefaultWebMapService.class, () -> mockWms)
                .withBean(
                        "wms_1_1_1_GetCapabilitiesResponse",
                        GetCapabilitiesResponse.class,
                        () -> mockGetCapabilities);
    }

    @Test
    void disabledByDefault() {
        withMockWMS()
                .run(
                        context -> {
                            assertThat(context)
                                    .doesNotHaveBean(CachingExtendedCapabilitiesProvider.class);
                            assertThat(context).doesNotHaveBean(ForwardGetMapToGwcAspect.class);
                            assertThat(context)
                                    .doesNotHaveBean(
                                            "gwcSettingsPageWMSIntegationDisabledCssContribution");
                        });
    }

    @Test
    void enabledByConfigAndGeoServerWebMapServiceNotFound() {
        runner.withPropertyValues("gwc.wms-integration=true")
                .run(
                        context -> {
                            assertThat(context)
                                    .doesNotHaveBean(CachingExtendedCapabilitiesProvider.class);
                            assertThat(context).doesNotHaveBean(ForwardGetMapToGwcAspect.class);
                            assertThat(context)
                                    .doesNotHaveBean(
                                            "gwcSettingsPageWMSIntegationDisabledCssContribution");
                        });
    }

    @Test
    void enabledByConfigAndGeoServerWebMapServiceFound() {
        withMockWMS()
                .withPropertyValues("gwc.wms-integration=true")
                .run(
                        context -> {
                            assertThat(context)
                                    .hasSingleBean(CachingExtendedCapabilitiesProvider.class);
                            assertThat(context).hasSingleBean(ForwardGetMapToGwcAspect.class);
                            assertThat(context)
                                    .doesNotHaveBean(
                                            "gwcSettingsPageWMSIntegationDisabledCssContribution");
                        });
    }

    @Test
    void disabledContributesCssToHideWebUIComponents() {
        withMockWMS()
                .withPropertyValues("geoserver.web-ui.gwc.enabled=true")
                .run(
                        context -> {
                            assertThat(context)
                                    .doesNotHaveBean(CachingExtendedCapabilitiesProvider.class);
                            assertThat(context).doesNotHaveBean(ForwardGetMapToGwcAspect.class);
                            assertThat(context)
                                    .hasBean("gwcSettingsPageWMSIntegationDisabledCssContribution");
                            HeaderContribution contrib =
                                    context.getBean(
                                            "gwcSettingsPageWMSIntegationDisabledCssContribution",
                                            HeaderContribution.class);
                            assertEquals(GWCSettingsPage.class, contrib.getScope());
                            assertEquals(
                                    "wms-integration-disabled.css", contrib.getCSS().getName());
                        });
    }
}
