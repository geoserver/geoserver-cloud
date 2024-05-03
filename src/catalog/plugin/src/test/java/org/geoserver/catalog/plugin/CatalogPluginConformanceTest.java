/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import org.geoserver.platform.GeoServerResourceLoader;

import java.io.File;

class CatalogPluginConformanceTest extends CatalogConformanceTest {

    protected @Override CatalogPlugin createCatalog(File tmpFolder) {
        var catalog = new org.geoserver.catalog.plugin.CatalogPlugin();
        catalog.setResourceLoader(new GeoServerResourceLoader(tmpFolder));
        catalog.addListener(new CatalogPluginStyleResourcePersister(catalog));
        return catalog;
    }
}
