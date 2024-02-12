/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.bus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.jackson.databind.catalog.GeoServerCatalogModule;
import org.geoserver.jackson.databind.config.GeoServerConfigModule;
import org.geoserver.platform.config.UpdateSequence;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.bus.BusBridge;
import org.springframework.cloud.bus.ServiceMatcher;

/**
 * @since 1.0
 */
class RemoteGeoServerEventsConfigurationTest {

    private final ServiceMatcher mockServiceMatcher = mock(ServiceMatcher.class);
    private final BusBridge mockBusBridge = mock(BusBridge.class);
    private final Catalog mockCatalog = mock(Catalog.class);
    private final GeoServer mockGeoserver = mock(GeoServer.class);
    private final UpdateSequence updateSequence = mock(UpdateSequence.class);

    private ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withBean(ServiceMatcher.class, () -> mockServiceMatcher)
                    .withBean(BusBridge.class, () -> mockBusBridge)
                    .withBean(UpdateSequence.class, () -> updateSequence)
                    .withBean("rawCatalog", Catalog.class, () -> mockCatalog)
                    .withBean("Geoserver", GeoServer.class, () -> mockGeoserver)
                    .withConfiguration(AutoConfigurations.of(BusAutoConfiguration.class))
                    .withConfiguration(
                            UserConfigurations.of(RemoteGeoServerEventsConfiguration.class));

    @Test
    void enabledByDefault() {
        runner.run(
                context -> {
                    assertThat(context).hasSingleBean(GeoServerCatalogModule.class);
                    assertThat(context).hasSingleBean(GeoServerConfigModule.class);
                    assertThat(context).hasSingleBean(InfoEventResolver.class);
                    assertThat(context).hasSingleBean(RemoteGeoServerEventMapper.class);
                    assertThat(context).hasSingleBean(RemoteGeoServerEventBridge.class);
                });
    }
}
