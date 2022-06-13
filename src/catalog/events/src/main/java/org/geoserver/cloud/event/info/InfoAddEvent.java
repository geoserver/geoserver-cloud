/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.info;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.geoserver.catalog.Info;
import org.geoserver.cloud.event.catalog.CatalogInfoAddEvent;
import org.geoserver.cloud.event.config.ConfigInfoAddEvent;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CatalogInfoAddEvent.class, name = "CatalogInfoAdded"),
    @JsonSubTypes.Type(value = ConfigInfoAddEvent.class),
})
public abstract class InfoAddEvent<SELF, S, I extends Info> extends InfoEvent<SELF, S, I> {

    private @Getter @Setter @NonNull I object;

    protected InfoAddEvent() {}

    protected InfoAddEvent(S source, S target, @NonNull I object) {
        super(source, target, resolveId(object), typeOf(object));
        this.object = object;
    }
}
