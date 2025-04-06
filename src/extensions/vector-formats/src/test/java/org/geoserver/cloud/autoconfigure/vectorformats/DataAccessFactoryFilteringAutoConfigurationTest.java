/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.vectorformats;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Iterators;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geotools.api.data.DataAccessFactory;
import org.geotools.api.data.DataAccessFinder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.vfny.geoserver.util.DataStoreUtils;

/**
 * Tests for {@link DataAccessFactoryFilteringAutoConfiguration}.
 */
class DataAccessFactoryFilteringAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataAccessFactoryFilteringAutoConfiguration.class));

    private static Map<String, DataAccessFactory> available;

    @BeforeAll
    static void findAvailableFactories() {
        // loads both DataAccessFactory and DataStoreFactorySpi
        available = Stream.of(Iterators.toArray(DataAccessFinder.getAvailableDataStores(), DataAccessFactory.class))
                .collect(Collectors.toMap(DataAccessFactory::getDisplayName, Function.identity()));
        assertThat(available)
                .containsKey("PostGIS")
                .containsKey("Oracle NG")
                .containsKey("Directory of spatial files (shapefiles)");
    }

    @Test
    void testAutoConfigurationEnabled() {
        contextRunner
                .withPropertyValues(
                        "geotools.data.filtering.enabled=true",
                        "geotools.data.filtering.vector-formats.[PostGIS]=true",
                        "geotools.data.filtering.vector-formats.[Oracle NG]=false",
                        "test.wfs.enabled=true")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(DataAccessFactoryFilteringAutoConfiguration.class)
                            .hasSingleBean(DataAccessFactoryFilterConfigProperties.class);

                    // Verify the processor bean is registered
                    assertThat(context.getBean("dataAccessFactoryFilterProcessor"))
                            .isNotNull();

                    // Verify configuration beans
                    assertThat(context.getBeansOfType(DataAccessFactoryFilterConfigProperties.class))
                            .isNotEmpty();

                    // Verify factory registry has been populated
                    DataAccessFactoryFilterConfigProperties config =
                            context.getBean(DataAccessFactoryFilterConfigProperties.class);
                    assertThat(config.isEnabled()).isTrue();

                    // Verify the vector formats map has been set up correctly
                    assertThat(config.getVectorFormats()).containsKey("PostGIS");
                    assertThat(config.getVectorFormats()).containsKey("Oracle NG");

                    // Check configuration matches what we set in properties
                    assertThat(config.getVectorFormats().get("PostGIS")).isTrue();
                    assertThat(config.getVectorFormats().get("Oracle NG")).isFalse();

                    // Verify defaults for unknown factories
                    assertThat(config.isFactoryEnabled("Unknown Factory")).isTrue();
                });
    }

    /**
     * Test {@code DataStoreUtils.getAvailableDataStoreFactories()}, which is how
     * GeoServer finds available factories, but saves them in a static cache
     */
    @Test
    void testDataStoreUtils() {
        contextRunner
                .withPropertyValues(
                        "geotools.data.filtering.enabled=true",
                        "geotools.data.filtering.vector-formats.[PostGIS]=false",
                        "geotools.data.filtering.vector-formats.[Oracle NG]=false")
                .run(context -> {
                    Map<String, DataAccessFactory> dsUtilsReported =
                            DataStoreUtils.getAvailableDataStoreFactories().stream()
                                    .collect(Collectors.toMap(DataAccessFactory::getDisplayName, Function.identity()));

                    assertThat(dsUtilsReported)
                            .containsKey("Directory of spatial files (shapefiles)")
                            .doesNotContainKey("PostGIS")
                            .doesNotContainKey("Oracle NG");
                });
    }

    @Test
    void testAutoConfigurationDisabled() {
        contextRunner
                .withPropertyValues("geotools.data.filtering.enabled=false")
                .run(context -> {
                    // Should not load auto-configuration beans when disabled
                    assertThat(context).doesNotHaveBean(DataAccessFactoryFilteringAutoConfiguration.class);
                });
    }

    @Test
    void testPropertyPlaceholderResolution() {
        contextRunner
                .withPropertyValues(
                        "test.customfactory.enabled=true",
                        "geotools.data.filtering.vector-formats.[PostGIS]=${test.postgis.enabled}",
                        "test.postgis.enabled=false")
                .run(context -> {
                    DataAccessFactoryFilterConfigProperties config =
                            context.getBean(DataAccessFactoryFilterConfigProperties.class);

                    // Verify property placeholder is resolved correctly
                    assertThat(config.isFactoryEnabled("PostGIS")).isFalse();

                    assertThat(config.isFactoryEnabled("Oracle NG")).isTrue();
                });
    }
}
