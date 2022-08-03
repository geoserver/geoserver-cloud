/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.wms.extensions;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.platform.ModuleStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Test suite for {@link VectorTilesConfiguration}
 *
 * @since 1.0
 */
public class VectorTilesConfigurationTest {

    private ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    // .withBean(WmsExtensionsConfigProperties.class)
                    .withConfiguration(AutoConfigurations.of(VectorTilesConfiguration.class));

    @Test
    void allEnabledByDefault() {
        testAllEnabled();
    }

    @Test
    void allEnabledExplicitly() {
        contextRunner =
                contextRunner.withPropertyValues(
                        "geoserver.wms.output-formats.vector-tiles.mapbox.enabled=true",
                        "geoserver.wms.output-formats.vector-tiles.geojson.enabled=true",
                        "geoserver.wms.output-formats.vector-tiles.topojson.enabled=true");
        testAllEnabled();
    }

    private void testAllEnabled() {
        hasBeans(
                "VectorTilesExtension",
                "wmsMapBoxBuilderFactory",
                "wmsMapBoxMapOutputFormat",
                "wmsGeoJsonBuilderFactory",
                "wmsGeoJsonMapOutputFormat",
                "wmsGeoJsonBuilderFactory",
                "wmsGeoJsonMapOutputFormat");
    }

    @Test
    void enableOne() {
        contextRunner =
                contextRunner.withPropertyValues(
                        "geoserver.wms.output-formats.vector-tiles.mapbox.enabled=true",
                        "geoserver.wms.output-formats.vector-tiles.geojson.enabled=false",
                        "geoserver.wms.output-formats.vector-tiles.topojson.enabled=false");

        hasBeans("VectorTilesExtension", "wmsMapBoxBuilderFactory", "wmsMapBoxMapOutputFormat");
        doesNotHaveBeans(
                "wmsGeoJsonBuilderFactory",
                "wmsGeoJsonMapOutputFormat",
                "wmsGeoJsonBuilderFactory",
                "wmsGeoJsonMapOutputFormat");
    }

    @Test
    void allDisabled() {
        contextRunner =
                contextRunner.withPropertyValues(
                        "geoserver.wms.output-formats.vector-tiles.mapbox.enabled=false",
                        "geoserver.wms.output-formats.vector-tiles.geojson.enabled=false",
                        "geoserver.wms.output-formats.vector-tiles.topojson.enabled=false");

        hasBeans("VectorTilesExtension");
        doesNotHaveBeans(
                "wmsMapBoxBuilderFactory",
                "wmsMapBoxMapOutputFormat",
                "wmsGeoJsonBuilderFactory",
                "wmsGeoJsonMapOutputFormat",
                "wmsGeoJsonBuilderFactory",
                "wmsGeoJsonMapOutputFormat");

        contextRunner.run(
                context ->
                        assertThat(context)
                                .getBean("VectorTilesExtension", ModuleStatus.class)
                                .hasFieldOrPropertyWithValue("enabled", false));
    }

    private void hasBeans(String... beans) {
        contextRunner.run(
                context -> {
                    for (String bean : beans) {
                        assertThat(context).hasBean(bean);
                    }
                });
    }

    private void doesNotHaveBeans(String... beans) {
        contextRunner.run(
                context -> {
                    for (String bean : beans) {
                        assertThat(context).doesNotHaveBean(bean);
                    }
                });
    }
}
