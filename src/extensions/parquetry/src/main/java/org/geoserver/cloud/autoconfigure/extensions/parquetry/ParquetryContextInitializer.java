/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.parquetry;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Disables the Stac-GeoParquet and Iceberg datastores, which are not production ready, before any bean loads and hence
 * before any {@code DataStoreFinder} lookup can see their factories as available.
 *
 * <p>Each parquetry-geotools factory reports itself unavailable when its disable system property is {@code true}. A
 * property that is already set is left untouched: launching with {@code -Dparquetry.geotools.iceberg.disabled=false}
 * re-enables the store. Setting the properties is idempotent across the webapp and actuator contexts.
 *
 * @since 3.1.0
 */
public class ParquetryContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final String STAC_GEOPARQUET_DISABLED = "parquetry.geotools.stac-geoparquet.disabled";
    static final String ICEBERG_DISABLED = "parquetry.geotools.iceberg.disabled";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        disableUnlessExplicitlySet(STAC_GEOPARQUET_DISABLED);
        disableUnlessExplicitlySet(ICEBERG_DISABLED);
    }

    private void disableUnlessExplicitlySet(String propertyName) {
        if (System.getProperty(propertyName) == null) {
            System.setProperty(propertyName, "true");
        }
    }
}
