/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.DefaultCatalogFacade;

public class CloudCatalogFacade extends DefaultCatalogFacade {

    public CloudCatalogFacade(Catalog rawCatalog) {
        super(rawCatalog);
    }

    public @Override void resolve() {
        // no-op
    }
}
