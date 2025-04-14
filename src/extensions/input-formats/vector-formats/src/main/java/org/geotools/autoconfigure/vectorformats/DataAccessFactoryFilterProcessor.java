/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.autoconfigure.vectorformats;

import java.util.Iterator;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.data.DataAccessFactory;
import org.geotools.api.data.DataAccessFinder;
import org.geotools.api.data.DataStoreFactorySpi;
import org.geotools.api.data.DataStoreFinder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * A BeanPostProcessor that handles DataAccessFactory filtering by directly
 * deregistering disabled factories from DataAccessFinder and DataStoreFinder.
 *
 * <p>
 * This processor runs early in the application lifecycle, just after the
 * DataAccessFactoryFilterConfigProperties bean is created, which ensures
 * factories are deregistered before any beans that might use them.
 */
@Slf4j(topic = "org.geotools.autoconfigure.vectorformats")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataAccessFactoryFilterProcessor {

    public DataAccessFactoryFilterProcessor(DataAccessFactoryFilterConfigProperties config) {
        // Process immediately in the constructor to ensure it happens before
        // any other beans are initialized, especially CloudGeoServerLoaderProxy
        log.info("Initializing DataAccessFactory filtering");
        processDataAccessFactories(config);
    }

    /**
     * Processes DataAccessFactory implementations, deregistering any that are
     * disabled.
     *
     * @param config the configuration properties
     */
    private void processDataAccessFactories(DataAccessFactoryFilterConfigProperties config) {
        Iterator<DataAccessFactory> factories = DataAccessFinder.getAvailableDataStores();
        int totalCount = 0;
        int disabledCount = 0;

        while (factories.hasNext()) {
            DataAccessFactory factory = factories.next();
            totalCount++;
            String className = factory.getClass().getName();
            String displayName = factory.getDisplayName();

            // Check if it's enabled by display name
            boolean enabled = config.isFactoryEnabled(displayName);

            if (enabled) {
                log.info("DataAccessFactory factory enabled: {} ({})", displayName, className);
            } else {
                DataAccessFinder.deregisterFactory(factory);
                if (factory instanceof DataStoreFactorySpi dsf) {
                    DataStoreFinder.deregisterFactory(dsf);
                }
                log.info("DataAccessFactory factory disabled: {} ({})", displayName, className);
                disabledCount++;
            }
        }

        log.info("DataAccessFactory filtering: {} of {} factories disabled", disabledCount, totalCount);
    }
}
