/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.config.impl;

import java.io.File;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.DefaultCatalogFacade;
import org.geoserver.catalog.plugin.CatalogConformanceTest;
import org.geoserver.config.GeoServerResourcePersister;
import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/** {@link CatalogConformanceTest} for the traditional {@link CatalogImpl} with {@link DefaultCatalogFacade} */
@Execution(ExecutionMode.CONCURRENT)
class CatalogImplConformanceTest extends CatalogConformanceTest {

    protected @Override CatalogImpl createCatalog(File tmpFolder) {
        var catalog = new org.geoserver.catalog.impl.CatalogImpl();
        catalog.setResourceLoader(new GeoServerResourceLoader(tmpFolder));
        catalog.addListener(new GeoServerResourcePersister(catalog));
        return catalog;
    }
}
