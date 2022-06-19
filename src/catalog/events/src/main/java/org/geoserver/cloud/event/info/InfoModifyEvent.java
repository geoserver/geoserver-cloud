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
import org.springframework.core.style.ToStringCreator;

import java.util.stream.Collectors;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = InfoPreModifyEvent.class),
    @JsonSubTypes.Type(value = InfoPostModifyEvent.class),
})
public abstract class InfoModifyEvent<SELF, INFO extends Info> extends InfoEvent<SELF, INFO> {

    private @Getter @Setter @NonNull Patch patch;

    protected InfoModifyEvent() {}

    protected InfoModifyEvent(
            @NonNull String objectId, @NonNull ConfigInfoType objectType, @NonNull Patch patch) {
        super(objectId, objectType);
        this.patch = patch;
    }

    protected @Override ToStringCreator toStringBuilder() {
        return super.toStringBuilder()
                .append(
                        "changes",
                        getPatch().getPropertyNames().stream().collect(Collectors.joining(",")));
    }
}
