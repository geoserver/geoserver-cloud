/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

public class CatalogPluginConformanceTest extends CatalogConformanceTest {

    protected @Override CatalogPlugin createCatalog() {
        return new org.geoserver.catalog.plugin.CatalogPlugin();
    }
}
