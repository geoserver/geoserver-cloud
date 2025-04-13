/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.vectortiles;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
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
        contextRunner = new ApplicationContextRunner()
                .withBean("extensions", GeoServerExtensions.class)
                .withBean("geoServer", GeoServer.class, GeoServerImpl::new)
                .withConfiguration(AutoConfigurations.of(VectorTilesAutoConfiguration.class));
    }

    @Test
    void testAllFormatsEnabledByDefault() {
        contextRunner.withPropertyValues("geoserver.service.wms.enabled=true").run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(MapBoxTileBuilderFactory.class)
                    .hasSingleBean(GeoJsonBuilderFactory.class)
                    .hasSingleBean(TopoJSONBuilderFactory.class)
                    .hasBean("VectorTilesExtension")
                    .getBean("VectorTilesExtension", ModuleStatus.class)
                    .hasFieldOrPropertyWithValue("enabled", true);
        });
        contextRunner.withPropertyValues("geoserver.service.webui.enabled=true").run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(MapBoxTileBuilderFactory.class)
                    .hasSingleBean(GeoJsonBuilderFactory.class)
                    .hasSingleBean(TopoJSONBuilderFactory.class)
                    .hasBean("VectorTilesExtension")
                    .getBean("VectorTilesExtension", ModuleStatus.class)
                    .hasFieldOrPropertyWithValue("enabled", true);
        });
        contextRunner.withPropertyValues("geoserver.service.gwc.enabled=true").run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(MapBoxTileBuilderFactory.class)
                    .hasSingleBean(GeoJsonBuilderFactory.class)
                    .hasSingleBean(TopoJSONBuilderFactory.class)
                    .hasBean("VectorTilesExtension")
                    .getBean("VectorTilesExtension", ModuleStatus.class)
                    .hasFieldOrPropertyWithValue("enabled", true);
        });
    }

    @Test
    void testMapBoxFormatDisabled() {

        contextRunner
                .withPropertyValues(
                        "geoserver.service.wms.enabled=true", "geoserver.extension.vector-tiles.mapbox=false")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .doesNotHaveBean(MapBoxTileBuilderFactory.class)
                            .hasSingleBean(GeoJsonBuilderFactory.class)
                            .hasSingleBean(TopoJSONBuilderFactory.class)
                            .getBean("VectorTilesExtension", ModuleStatus.class)
                            .hasFieldOrPropertyWithValue("enabled", true);
                });

        contextRunner
                .withPropertyValues(
                        "geoserver.service.gwc.enabled=true", "geoserver.extension.vector-tiles.mapbox=false")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .doesNotHaveBean(MapBoxTileBuilderFactory.class)
                            .hasSingleBean(GeoJsonBuilderFactory.class)
                            .hasSingleBean(TopoJSONBuilderFactory.class)
                            .getBean("VectorTilesExtension", ModuleStatus.class)
                            .hasFieldOrPropertyWithValue("enabled", true);
                });

        contextRunner
                .withPropertyValues(
                        "geoserver.service.webui.enabled=true", "geoserver.extension.vector-tiles.mapbox=false")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .doesNotHaveBean(MapBoxTileBuilderFactory.class)
                            .hasSingleBean(GeoJsonBuilderFactory.class)
                            .hasSingleBean(TopoJSONBuilderFactory.class)
                            .getBean("VectorTilesExtension", ModuleStatus.class)
                            .hasFieldOrPropertyWithValue("enabled", true);
                });
    }

    @Test
    void testAllFormatsDisabled() {
        contextRunner
                .withPropertyValues(
                        "geoserver.service.wms.enabled=true",
                        "geoserver.extension.vector-tiles.mapbox=false",
                        "geoserver.extension.vector-tiles.geojson=false",
                        "geoserver.extension.vector-tiles.topojson=false")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .doesNotHaveBean(MapBoxTileBuilderFactory.class)
                            .doesNotHaveBean(GeoJsonBuilderFactory.class)
                            .doesNotHaveBean(TopoJSONBuilderFactory.class)
                            .getBean("VectorTilesExtension", ModuleStatusImpl.class)
                            .hasFieldOrPropertyWithValue("enabled", false);
                });

        contextRunner
                .withPropertyValues(
                        "geoserver.service.gwc.enabled=true",
                        "geoserver.extension.vector-tiles.mapbox=false",
                        "geoserver.extension.vector-tiles.geojson=false",
                        "geoserver.extension.vector-tiles.topojson=false")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .doesNotHaveBean(MapBoxTileBuilderFactory.class)
                            .doesNotHaveBean(GeoJsonBuilderFactory.class)
                            .doesNotHaveBean(TopoJSONBuilderFactory.class)
                            .getBean("VectorTilesExtension", ModuleStatusImpl.class)
                            .hasFieldOrPropertyWithValue("enabled", false);
                });

        contextRunner
                .withPropertyValues(
                        "geoserver.service.webui.enabled=true",
                        "geoserver.extension.vector-tiles.mapbox=false",
                        "geoserver.extension.vector-tiles.geojson=false",
                        "geoserver.extension.vector-tiles.topojson=false")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .doesNotHaveBean(MapBoxTileBuilderFactory.class)
                            .doesNotHaveBean(GeoJsonBuilderFactory.class)
                            .doesNotHaveBean(TopoJSONBuilderFactory.class)
                            .getBean("VectorTilesExtension", ModuleStatusImpl.class)
                            .hasFieldOrPropertyWithValue("enabled", false);
                });
    }
}
