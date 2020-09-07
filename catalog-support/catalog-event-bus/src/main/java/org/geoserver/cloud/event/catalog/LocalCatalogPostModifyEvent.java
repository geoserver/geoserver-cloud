/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.catalog;

import java.util.List;
import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.cloud.event.LocalPostModifyEvent;

public class LocalCatalogPostModifyEvent extends LocalPostModifyEvent<Catalog, CatalogInfo> {
    private static final long serialVersionUID = 1L;

    LocalCatalogPostModifyEvent(
            Catalog source,
            @NonNull CatalogInfo object,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {
        super(source, object, propertyNames, oldValues, newValues);
    }

    public static LocalCatalogPostModifyEvent of(Catalog catalog, CatalogPostModifyEvent event) {
        return new LocalCatalogPostModifyEvent(
                catalog,
                event.getSource(),
                event.getPropertyNames(),
                event.getOldValues(),
                event.getNewValues());
    }
}
