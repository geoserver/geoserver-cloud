/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.event.bus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.event.bus.InfoEventResolver;
import org.geoserver.cloud.event.bus.RemoteGeoServerEventBridge;
import org.geoserver.cloud.event.bus.RemoteGeoServerEventMapper;
import org.geoserver.config.GeoServer;
import org.geoserver.jackson.databind.catalog.GeoServerCatalogModule;
import org.geoserver.jackson.databind.config.GeoServerConfigModule;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.bus.BusBridge;
import org.springframework.cloud.bus.ServiceMatcher;

/**
 * @since 1.0
 */
class RemoteGeoServerEventsAutoConfigurationTest {

    private final ServiceMatcher mockServiceMatcher = mock(ServiceMatcher.class);
    private final BusBridge mockBusBridge = mock(BusBridge.class);
    private final Catalog mockCatalog = mock(Catalog.class);
    private final GeoServer mockGeoserver = mock(GeoServer.class);

    private ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withBean(ServiceMatcher.class, () -> mockServiceMatcher)
                    .withBean(BusBridge.class, () -> mockBusBridge)
                    .withBean("rawCatalog", Catalog.class, () -> mockCatalog)
                    .withBean("Geoserver", GeoServer.class, () -> mockGeoserver)
                    .withConfiguration(
                            AutoConfigurations.of(
                                    BusAutoConfiguration.class,
                                    RemoteGeoServerEventsAutoConfiguration.class));

    public @Test void enabledByDefault() {
        assertEnabled(runner);
    }

    public @Test void conditionalOnGeoServerRemoteEventsEnabled() {
        assertDisabled(runner.withPropertyValues("geoserver.bus.enabled: false"));
    }

    public @Test void conditionalOnBusEnabled() {
        assertDisabled(runner.withPropertyValues("spring.cloud.bus.enabled: false"));
    }

    public @Test void conditionalOnCatalogEvents() {
        assertDisabled(runner.withPropertyValues("geoserver.catalog.events.enabled: false"));
    }

    private void assertEnabled(ApplicationContextRunner runner) {

        runner.run(
                context -> {
                    assertThat(context).hasSingleBean(GeoServerCatalogModule.class);
                    assertThat(context).hasSingleBean(GeoServerConfigModule.class);
                    assertThat(context).hasSingleBean(InfoEventResolver.class);
                    assertThat(context).hasSingleBean(RemoteGeoServerEventMapper.class);
                    assertThat(context).hasSingleBean(RemoteGeoServerEventBridge.class);
                });
    }

    private void assertDisabled(ApplicationContextRunner runner) {

        runner.run(
                context -> {
                    assertThat(context).doesNotHaveBean(GeoServerCatalogModule.class);
                    assertThat(context).doesNotHaveBean(GeoServerConfigModule.class);
                    assertThat(context).doesNotHaveBean(InfoEventResolver.class);
                    assertThat(context).doesNotHaveBean(RemoteGeoServerEventMapper.class);
                    assertThat(context).doesNotHaveBean(RemoteGeoServerEventBridge.class);
                });
    }
}
