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
import org.geoserver.cloud.event.catalog.CatalogInfoPreModifyEvent;
import org.geoserver.cloud.event.config.ConfigInfoPreModifyEvent;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CatalogInfoPreModifyEvent.class),
    @JsonSubTypes.Type(value = ConfigInfoPreModifyEvent.class),
})
public abstract class InfoPreModifyEvent<SELF, INFO extends Info>
        extends InfoModifyEvent<SELF, INFO> {

    protected InfoPreModifyEvent() {}

    protected InfoPreModifyEvent(
            @NonNull String objectId, @NonNull ConfigInfoType objectType, @NonNull Patch patch) {
        super(objectId, objectType, patch);
    }
}
