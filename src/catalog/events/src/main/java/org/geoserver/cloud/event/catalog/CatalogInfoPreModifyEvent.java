/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.catalog;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.NonNull;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoPreModifyEvent;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("CatalogInfoPreModify")
public class CatalogInfoPreModifyEvent
        extends InfoPreModifyEvent<CatalogInfoPreModifyEvent, CatalogInfo> {

    protected CatalogInfoPreModifyEvent() {}

    protected CatalogInfoPreModifyEvent(
            @NonNull String objectId, @NonNull ConfigInfoType objectType, @NonNull Patch patch) {
        super(objectId, objectType, patch);
    }

    public static CatalogInfoPreModifyEvent createLocal(@NonNull CatalogModifyEvent event) {
        PropertyDiff diff =
                PropertyDiff.valueOf(
                        event.getPropertyNames(), event.getOldValues(), event.getNewValues());
        Patch patch = diff.toPatch();
        return new CatalogInfoPreModifyEvent(
                resolveId(event.getSource()), typeOf(event.getSource()), patch);
    }
}
