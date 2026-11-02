/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.tileverse.parquetry;

import static org.assertj.core.api.Assertions.assertThat;

import io.tileverse.parquetry.geotools.iceberg.IcebergDataStoreFactory;
import io.tileverse.parquetry.geotools.parquet.GeoParquetDataStoreFactory;
import io.tileverse.parquetry.geotools.parquet.StacDataStoreFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Test suite for {@link ParquetryContextInitializer}
 *
 * <p>The disable properties are JVM-global; each test starts from a clean slate and the original values are restored
 * afterwards to keep tests independent.
 */
class ParquetryContextInitializerTest {

    private static final String PREFIX = "geoserver.extension.tileverse.parquetry";

    private static final List<String> DISABLE_PROPERTIES = List.of(
            ParquetryContextInitializer.GEOPARQUET_DISABLED,
            ParquetryContextInitializer.STAC_GEOPARQUET_DISABLED,
            ParquetryContextInitializer.ICEBERG_DISABLED);

    private final Map<String, String> originalValues = new HashMap<>();

    @BeforeEach
    void clearDisableProperties() {
        for (String propertyName : DISABLE_PROPERTIES) {
            originalValues.put(propertyName, System.getProperty(propertyName));
            System.clearProperty(propertyName);
        }
    }

    @AfterEach
    void restoreDisableProperties() {
        originalValues.forEach(this::restore);
    }

    private void restore(String propertyName, String originalValue) {
        if (originalValue == null) {
            System.clearProperty(propertyName);
        } else {
            System.setProperty(propertyName, originalValue);
        }
    }

    private void runInitializer(String... environmentProperties) {
        GenericApplicationContext context = new GenericApplicationContext();
        TestPropertyValues.of(environmentProperties).applyTo(context);
        new ParquetryContextInitializer().initialize(context);
    }

    @Test
    void disablesStacAndIcebergWhenUnset() {
        runInitializer();
        assertThat(System.getProperty(ParquetryContextInitializer.STAC_GEOPARQUET_DISABLED))
                .isEqualTo("true");
        assertThat(System.getProperty(ParquetryContextInitializer.ICEBERG_DISABLED))
                .isEqualTo("true");
    }

    @Test
    void honorsExplicitOverride() {
        System.setProperty(ParquetryContextInitializer.ICEBERG_DISABLED, "false");
        runInitializer();
        assertThat(System.getProperty(ParquetryContextInitializer.ICEBERG_DISABLED))
                .isEqualTo("false");
        assertThat(System.getProperty(ParquetryContextInitializer.STAC_GEOPARQUET_DISABLED))
                .isEqualTo("true");
    }

    @Test
    void factoryAvailabilityFollowsTheDisableProperties() {
        runInitializer();
        assertThat(new GeoParquetDataStoreFactory().isAvailable()).isTrue();
        assertThat(new StacDataStoreFactory().isAvailable()).isFalse();
        assertThat(new IcebergDataStoreFactory().isAvailable()).isFalse();
    }

    @Test
    void leavesGeoParquetAloneByDefault() {
        runInitializer();
        assertThat(System.getProperty(ParquetryContextInitializer.GEOPARQUET_DISABLED))
                .isNull();
    }

    @Test
    void disablesGeoParquetWhenUmbrellaIsOff() {
        runInitializer(PREFIX + ".enabled=false");
        assertThat(System.getProperty(ParquetryContextInitializer.GEOPARQUET_DISABLED))
                .isEqualTo("true");
        assertThat(new GeoParquetDataStoreFactory().isAvailable()).isFalse();
    }

    @Test
    void disablesGeoParquetWhenParquetStoreIsOff() {
        runInitializer(PREFIX + ".parquet.enabled=false");
        assertThat(System.getProperty(ParquetryContextInitializer.GEOPARQUET_DISABLED))
                .isEqualTo("true");
        assertThat(new GeoParquetDataStoreFactory().isAvailable()).isFalse();
    }

    @Test
    void leavesGeoParquetAloneWhileOnlyOutputFormatsAreOff() {
        runInitializer(
                PREFIX + ".output-formats.geoparquet.enabled=false",
                PREFIX + ".output-formats.arrow-ipc.enabled=false");
        assertThat(System.getProperty(ParquetryContextInitializer.GEOPARQUET_DISABLED))
                .isNull();
        assertThat(new GeoParquetDataStoreFactory().isAvailable()).isTrue();
    }

    @Test
    void explicitGeoParquetOverrideWins() {
        System.setProperty(ParquetryContextInitializer.GEOPARQUET_DISABLED, "false");
        runInitializer(PREFIX + ".enabled=false");
        assertThat(System.getProperty(ParquetryContextInitializer.GEOPARQUET_DISABLED))
                .isEqualTo("false");
    }
}
