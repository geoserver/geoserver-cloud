/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.tileverse.parquetry;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * Makes parquetry-geotools datastores unavailable before any bean loads, and hence before any {@code DataStoreFinder}
 * lookup can see their factories as available:
 *
 * <ul>
 *   <li>the Stac-GeoParquet and Iceberg datastores always, as they are not production ready;
 *   <li>the GeoParquet datastore when the plugin's umbrella switch or its {@code parquet.enabled} toggle is off (see
 *       {@link ParquetryConfigProperties}). The factory is discovered through SPI rather than as a Spring bean, and
 *       Spring conditionals alone cannot hide it.
 * </ul>
 *
 * <p>Each parquetry-geotools factory reports itself unavailable when its disable system property is {@code true}. A
 * property that is already set is left untouched: launching with {@code -Dparquetry.geotools.iceberg.disabled=false}
 * re-enables the store. Setting the properties is idempotent across the webapp and actuator contexts.
 *
 * @since 3.1.0
 */
public class ParquetryContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final String GEOPARQUET_DISABLED = "parquetry.geotools.geoparquet.disabled";
    static final String STAC_GEOPARQUET_DISABLED = "parquetry.geotools.stac-geoparquet.disabled";
    static final String ICEBERG_DISABLED = "parquetry.geotools.iceberg.disabled";

    private static final String PLUGIN_ENABLED = ParquetryConfigProperties.PREFIX + ".enabled";
    private static final String PARQUET_STORE_ENABLED = ParquetryConfigProperties.PREFIX + ".parquet.enabled";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        disableUnlessExplicitlySet(STAC_GEOPARQUET_DISABLED);
        disableUnlessExplicitlySet(ICEBERG_DISABLED);
        if (!parquetStoreEnabled(applicationContext.getEnvironment())) {
            disableUnlessExplicitlySet(GEOPARQUET_DISABLED);
        }
    }

    private boolean parquetStoreEnabled(Environment environment) {
        boolean pluginEnabled = environment.getProperty(PLUGIN_ENABLED, Boolean.class, true);
        boolean storeEnabled = environment.getProperty(PARQUET_STORE_ENABLED, Boolean.class, true);
        return pluginEnabled && storeEnabled;
    }

    private void disableUnlessExplicitlySet(String propertyName) {
        if (System.getProperty(propertyName) == null) {
            System.setProperty(propertyName, "true");
        }
    }
}
