/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.config.impl;

import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.DefaultCatalogFacade;
import org.geoserver.catalog.plugin.CatalogConformanceTest;

/**
 * {@link CatalogConformanceTest} for the traditional {@link CatalogImpl} with {@link
 * DefaultCatalogFacade}
 */
class CatalogImplConformanceTest extends CatalogConformanceTest {

    protected @Override CatalogImpl createCatalog() {
        return new org.geoserver.catalog.impl.CatalogImpl();
    }
}
