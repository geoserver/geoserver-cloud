/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.info;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.NonNull;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.event.catalog.CatalogInfoModifyEvent;
import org.geoserver.cloud.event.config.ConfigInfoModifyEvent;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CatalogInfoModifyEvent.class, name = "CatalogInfoModified"),
    @JsonSubTypes.Type(value = ConfigInfoModifyEvent.class),
})
public abstract class InfoPostModifyEvent<SELF, INFO extends Info>
        extends InfoModifyEvent<SELF, INFO> {

    protected InfoPostModifyEvent() {}

    protected InfoPostModifyEvent(
            @NonNull String objectId, @NonNull ConfigInfoType objectType, @NonNull Patch patch) {
        super(objectId, objectType, patch);
    }
}
