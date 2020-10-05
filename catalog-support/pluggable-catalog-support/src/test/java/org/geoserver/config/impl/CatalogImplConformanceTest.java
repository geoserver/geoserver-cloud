/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.config.impl;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogConformanceTest;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.DefaultCatalogFacade;

/**
 * {@link CatalogConformanceTest} for the traditional {@link CatalogImpl} with {@link
 * DefaultCatalogFacade}
 */
public class CatalogImplConformanceTest extends CatalogConformanceTest {

    protected @Override Catalog createCatalog() {
        return new org.geoserver.catalog.impl.CatalogImpl();
    }
}
