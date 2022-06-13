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
import org.geoserver.catalog.plugin.Patch;

import javax.annotation.Nullable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = InfoPreModifyEvent.class),
    @JsonSubTypes.Type(value = InfoPostModifyEvent.class),
})
public abstract class InfoModifyEvent<SELF, SOURCE, INFO extends Info>
        extends InfoEvent<SELF, SOURCE, INFO> {

    private @Getter @Setter @NonNull Patch patch;

    protected InfoModifyEvent() {}

    protected InfoModifyEvent(
            @Nullable SOURCE source,
            @Nullable SOURCE target,
            @NonNull String objectId,
            @NonNull ConfigInfoType objectType,
            @NonNull Patch patch) {
        super(source, target, objectId, objectType);
        this.patch = patch;
    }

    @Override
    public String toString() {
        return String.format(
                "%s[%s(%s), changed: %s]",
                getClass().getSimpleName(),
                getObjectType(),
                getObjectId(),
                getPatch().getPropertyNames());
    }

    //    public @Override String toString() {
    //        return String.format(
    //                "%s(remote: %s, type: %s, id: %s, source: %s, target: %s, patch: %s)",
    //                getClass().getSimpleName(),
    //                isRemote(),
    //                getObjectType(),
    //                getObjectId(),
    //                getSource() == null ? null : getSource().getClass().getSimpleName(),
    //                getTarget() == null ? null : getTarget().getClass().getSimpleName(),
    //                getPatch());
    //    }
}
