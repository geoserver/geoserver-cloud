/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.cloud.test.CatalogConformanceTest;

public class CatalogImplConformanceTest extends CatalogConformanceTest {

    protected @Override Catalog createCatalog() {
        CatalogImpl catalog = new org.geoserver.catalog.plugin.CatalogImpl();
        //        org.geoserver.catalog.plugin.DefaultCatalogFacade facade =
        //                new org.geoserver.catalog.plugin.DefaultCatalogFacade();
        //        catalog.setFacade(facade);
        return catalog;
    }
    //
    //    /**
    //     * Override as no-op, {@link CatalogImpl} decorates the facade only on its default
    // constructor,
    //     * and {@code IsolatedCatalogFacade} is package private. It should also do so at {@link
    //     * CatalogImpl#setFacade}
    //     */
    //    public @Override void testAddIsolatedWorkspace() {}
    //
    //    /**
    //     * Override as no-op, {@link CatalogImpl} decorates the facade only on its default
    // constructor,
    //     * and {@code IsolatedCatalogFacade} is package private. It should also do so at {@link
    //     * CatalogImpl#setFacade}
    //     */
    //    public @Override void testAddIsolatedNamespace() {}
}
