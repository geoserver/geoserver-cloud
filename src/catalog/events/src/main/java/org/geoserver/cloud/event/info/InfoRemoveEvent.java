/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.info;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.NonNull;

import org.geoserver.catalog.Info;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoveEvent;
import org.geoserver.cloud.event.config.ConfigInfoRemoveEvent;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CatalogInfoRemoveEvent.class, name = "CatalogInfoRemoved"),
    @JsonSubTypes.Type(value = ConfigInfoRemoveEvent.class, name = "ConfigInfoRemoved"),
})
public abstract class InfoRemoveEvent<SELF, INFO extends Info> extends InfoEvent<SELF, INFO> {

    protected InfoRemoveEvent() {}

    protected InfoRemoveEvent(@NonNull String objectId, @NonNull ConfigInfoType type) {
        super(objectId, type);
    }
}
