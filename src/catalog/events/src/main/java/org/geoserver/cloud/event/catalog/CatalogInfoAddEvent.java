/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.catalog;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.NonNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.cloud.event.info.InfoAddEvent;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("CatalogInfoAdded")
public class CatalogInfoAddEvent extends InfoAddEvent<CatalogInfoAddEvent, Catalog, CatalogInfo> {

    protected CatalogInfoAddEvent() {}

    CatalogInfoAddEvent(Catalog source, Catalog target, @NonNull CatalogInfo object) {
        super(source, target, object);
    }

    public static CatalogInfoAddEvent createLocal(
            @NonNull Catalog source, @NonNull CatalogAddEvent event) {
        return new CatalogInfoAddEvent(source, null, event.getSource());
    }
}
