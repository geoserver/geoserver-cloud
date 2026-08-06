/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.parquetry;

import static org.assertj.core.api.Assertions.assertThat;

import io.tileverse.parquetry.geotools.iceberg.IcebergDataStoreFactory;
import io.tileverse.parquetry.geotools.parquet.GeoParquetDataStoreFactory;
import io.tileverse.parquetry.geotools.parquet.StacDataStoreFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Test suite for {@link ParquetryContextInitializer}
 *
 * <p>The disable properties are JVM-global; each test starts from a clean slate and the original values are restored
 * afterwards to keep tests independent.
 */
class ParquetryContextInitializerTest {

    private String originalStacValue;
    private String originalIcebergValue;

    @BeforeEach
    void clearDisableProperties() {
        originalStacValue = System.getProperty(ParquetryContextInitializer.STAC_GEOPARQUET_DISABLED);
        originalIcebergValue = System.getProperty(ParquetryContextInitializer.ICEBERG_DISABLED);
        System.clearProperty(ParquetryContextInitializer.STAC_GEOPARQUET_DISABLED);
        System.clearProperty(ParquetryContextInitializer.ICEBERG_DISABLED);
    }

    @AfterEach
    void restoreDisableProperties() {
        restore(ParquetryContextInitializer.STAC_GEOPARQUET_DISABLED, originalStacValue);
        restore(ParquetryContextInitializer.ICEBERG_DISABLED, originalIcebergValue);
    }

    private void restore(String propertyName, String originalValue) {
        if (originalValue == null) {
            System.clearProperty(propertyName);
        } else {
            System.setProperty(propertyName, originalValue);
        }
    }

    private void runInitializer() {
        new ParquetryContextInitializer().initialize(new GenericApplicationContext());
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
}
