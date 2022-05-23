/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.catalog;

import lombok.NonNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.cloud.event.LocalPreModifyEvent;

import java.util.List;

public class LocalCatalogPreModifyEvent extends LocalPreModifyEvent<Catalog, CatalogInfo> {
    private static final long serialVersionUID = 1L;

    LocalCatalogPreModifyEvent(
            Catalog source,
            @NonNull CatalogInfo object,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {
        super(source, object, propertyNames, oldValues, newValues);
    }

    public static LocalCatalogPreModifyEvent of(Catalog catalog, CatalogModifyEvent event) {
        return new LocalCatalogPreModifyEvent(
                catalog,
                event.getSource(),
                event.getPropertyNames(),
                event.getOldValues(),
                event.getNewValues());
    }
}
