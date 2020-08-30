/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;

public class AbstractCatalogFacadeTest extends org.geoserver.catalog.impl.CatalogImplTest {

    @Override
    protected Catalog createCatalog() {
        CatalogImpl catalog = new CatalogImpl();
        catalog.setFacade(new DefaultCatalogFacade(catalog));
        return catalog;
    }

    /**
     * Override as no-op, {@link CatalogImpl} decorates the facade only on its default constructor,
     * and {@code IsolatedCatalogFacade} is package private. It should also do so at {@link
     * CatalogImpl#setFacade}
     */
    public @Override void testAddIsolatedWorkspace() {}

    /**
     * Override as no-op, {@link CatalogImpl} decorates the facade only on its default constructor,
     * and {@code IsolatedCatalogFacade} is package private. It should also do so at {@link
     * CatalogImpl#setFacade}
     */
    public @Override void testAddIsolatedNamespace() {}
}
