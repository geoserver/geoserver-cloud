/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.vectortiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.WMS;
import org.geoserver.wms.geojson.GeoJsonBuilderFactory;
import org.geoserver.wms.mapbox.MapBoxTileBuilderFactory;
import org.geoserver.wms.topojson.TopoJSONBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Test suite for {@link VectorTilesAutoConfiguration}
 */
class VectorTilesAutoConfigurationTest {

    private ApplicationContextRunner contextRunner;

    @BeforeEach
    void setUp() {
        // Create a mock GeoServer instance and WMS dependencies to satisfy @ConditionalOnGeoServerWMS
        var mockGeoServer = mock(GeoServer.class);
        var mockWMS = mock(WMS.class);
        var mockWmsServiceTarget = mock(DefaultWebMapService.class);

        contextRunner = new ApplicationContextRunner()
                .withBean("extensions", GeoServerExtensions.class)
                .withBean("geoServer", GeoServer.class, () -> mockGeoServer)
                .withBean("wmsServiceTarget", DefaultWebMapService.class, () -> mockWmsServiceTarget)
                .withBean(WMS.class, () -> mockWMS)
                .withConfiguration(AutoConfigurations.of(VectorTilesAutoConfiguration.class));
    }

    @Test
    void testAllFormatsEnabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context.getBean("VectorTilesExtension", ModuleStatusImpl.class)
                            .isEnabled())
                    .isTrue();
            assertThat(context).hasSingleBean(MapBoxTileBuilderFactory.class);
            assertThat(context).hasSingleBean(GeoJsonBuilderFactory.class);
            assertThat(context).hasSingleBean(TopoJSONBuilderFactory.class);
        });
    }

    @Test
    void testMapBoxFormatDisabled() {
        contextRunner
                .withPropertyValues("geoserver.extension.vector-tiles.mapbox=false")
                .run(context -> {
                    // Module is still enabled because other formats are enabled
                    assertThat(context.getBean("VectorTilesExtension", ModuleStatusImpl.class)
                                    .isEnabled())
                            .isTrue();
                    assertThat(context).doesNotHaveBean(MapBoxTileBuilderFactory.class);
                    assertThat(context).hasSingleBean(GeoJsonBuilderFactory.class);
                    assertThat(context).hasSingleBean(TopoJSONBuilderFactory.class);
                });
    }

    @Test
    void testAllFormatsDisabled() {
        contextRunner
                .withPropertyValues(
                        "geoserver.extension.vector-tiles.mapbox=false",
                        "geoserver.extension.vector-tiles.geojson=false",
                        "geoserver.extension.vector-tiles.topojson=false")
                .run(context -> {
                    // Module is disabled because all formats are disabled
                    assertThat(context.getBean("VectorTilesExtension", ModuleStatusImpl.class)
                                    .isEnabled())
                            .isFalse();
                    assertThat(context).doesNotHaveBean(MapBoxTileBuilderFactory.class);
                    assertThat(context).doesNotHaveBean(GeoJsonBuilderFactory.class);
                    assertThat(context).doesNotHaveBean(TopoJSONBuilderFactory.class);
                });
    }
}
