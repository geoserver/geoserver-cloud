/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.autoconfigure.rasterformats;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Tests for {@link GridFormatFactoryFilteringAutoConfiguration}.
 */
class GridFormatFactoryFilteringAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GridFormatFactoryFilteringAutoConfiguration.class));

    @Test
    void testAutoConfigurationEnabled() {
        contextRunner
                .withPropertyValues(
                        "geotools.data.filtering.enabled=true",
                        "geotools.data.filtering.raster-formats.GeoTIFF=true",
                        "geotools.data.filtering.raster-formats.ArcGrid=false")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(GridFormatFactoryFilteringAutoConfiguration.class)
                            .hasSingleBean(GridFormatFactoryFilterConfigProperties.class);

                    // Verify the processor bean is registered
                    assertThat(context.getBean("gridFormatFactoryFilterProcessor"))
                            .isNotNull();

                    // Verify configuration beans
                    assertThat(context.getBeansOfType(GridFormatFactoryFilterConfigProperties.class))
                            .isNotEmpty();

                    // Verify factory registry has been populated
                    GridFormatFactoryFilterConfigProperties config =
                            context.getBean(GridFormatFactoryFilterConfigProperties.class);
                    assertThat(config.isEnabled()).isTrue();

                    // Verify the formats map has been set up correctly
                    assertThat(config.getRasterFormats()).containsEntry("GeoTIFF", true);
                    assertThat(config.getRasterFormats()).containsEntry("ArcGrid", false);

                    // Verify defaults for unknown factories
                    assertThat(config.isFormatEnabled("Unknown Format")).isTrue();
                });
    }

    @Test
    void testAutoConfigurationDisabled() {
        contextRunner
                .withPropertyValues("geotools.data.filtering.enabled=false")
                .run(context -> {
                    // Should not load auto-configuration beans when disabled
                    assertThat(context).doesNotHaveBean(GridFormatFactoryFilteringAutoConfiguration.class);
                });
    }

    @Test
    void testPropertyPlaceholderResolution() {
        contextRunner
                .withPropertyValues(
                        "test.geotiff.enabled=false",
                        "geotools.data.filtering.raster-formats.GeoTIFF=${test.geotiff.enabled}")
                .run(context -> {
                    GridFormatFactoryFilterConfigProperties config =
                            context.getBean(GridFormatFactoryFilterConfigProperties.class);

                    // Verify property placeholder is resolved correctly
                    assertThat(config.isFormatEnabled("GeoTIFF")).isFalse();
                });
    }
}
