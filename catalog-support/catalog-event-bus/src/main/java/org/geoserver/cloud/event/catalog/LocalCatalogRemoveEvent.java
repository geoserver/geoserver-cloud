/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.catalog;

import lombok.NonNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.cloud.event.LocalRemoveEvent;

public class LocalCatalogRemoveEvent extends LocalRemoveEvent<Catalog, CatalogInfo> {
    private static final long serialVersionUID = 1L;

    public LocalCatalogRemoveEvent(Catalog source, @NonNull CatalogInfo object) {
        super(source, object);
    }

    public static LocalCatalogRemoveEvent of(Catalog catalog, CatalogRemoveEvent event) {
        return new LocalCatalogRemoveEvent(catalog, event.getSource());
    }
}
