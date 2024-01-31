/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import org.geoserver.config.ServiceInfo;
import org.springframework.lang.Nullable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("ServiceInfoRemoved")
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("serial")
public class ServiceRemoved extends ConfigInfoRemoved {

    private @Getter @Nullable String workspaceId;

    protected ServiceRemoved() {
        // default constructor, needed for deserialization
    }

    protected ServiceRemoved(
            long updateSequence, @NonNull ServiceInfo info, @Nullable String workspaceId) {

        super(updateSequence, resolveId(info), prefixedName(info), typeOf(info));
        this.workspaceId = workspaceId;
    }

    public static ServiceRemoved createLocal(long updateSequence, @NonNull ServiceInfo info) {
        final String workspaceId = resolveNullableId(info.getWorkspace());
        return new ServiceRemoved(updateSequence, info, workspaceId);
    }
}
