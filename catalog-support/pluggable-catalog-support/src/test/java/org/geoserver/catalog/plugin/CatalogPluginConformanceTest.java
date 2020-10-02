/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogConformanceTest;

public class CatalogPluginConformanceTest extends CatalogConformanceTest {

    protected @Override Catalog createCatalog() {
        return new org.geoserver.catalog.plugin.CatalogImpl();
    }
}
