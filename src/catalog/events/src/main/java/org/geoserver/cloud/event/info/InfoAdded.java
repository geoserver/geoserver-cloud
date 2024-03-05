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
import org.geoserver.cloud.event.catalog.CatalogInfoAdded;
import org.geoserver.cloud.event.config.ConfigInfoAdded;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CatalogInfoAdded.class),
    @JsonSubTypes.Type(value = ConfigInfoAdded.class),
})
@SuppressWarnings("serial")
public abstract class InfoAdded<I extends Info> extends InfoEvent {

    private @Getter @Setter I object;

    protected InfoAdded() {}

    protected InfoAdded(long updateSequence, @NonNull I object) {
        this(updateSequence, resolveId(object), prefixedName(object), typeOf(object), object);
    }

    protected InfoAdded(
            long updateSequence,
            @NonNull String id,
            @NonNull String prefixedName,
            @NonNull ConfigInfoType type,
            @NonNull I object) {
        super(updateSequence, id, prefixedName, type);
        this.object = object;
    }
}
