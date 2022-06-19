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

import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.springframework.core.style.ToStringCreator;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("ServiceInfoModified")
@EqualsAndHashCode(callSuper = true)
public class ServiceInfoModifyEvent
        extends ConfigInfoModifyEvent<ServiceInfoModifyEvent, LoggingInfo>
        implements ConfigInfoEvent {

    private @Getter String workspaceId;

    protected ServiceInfoModifyEvent() {
        // default constructor, needed for deserialization
    }

    protected ServiceInfoModifyEvent(
            @NonNull String objectId, @NonNull Patch patch, String workspaceId) {

        super(objectId, ConfigInfoType.ServiceInfo, patch);
        this.workspaceId = workspaceId;
    }

    protected @Override ToStringCreator toStringBuilder() {
        return super.toStringBuilder().append("workspace", getWorkspaceId());
    }

    public static ServiceInfoModifyEvent createLocal(
            @NonNull ServiceInfo object, @NonNull Patch patch) {

        final @NonNull String serviceId = InfoEvent.resolveId(object);
        final String workspaceId = InfoEvent.resolveId(object.getWorkspace());
        return new ServiceInfoModifyEvent(serviceId, patch, workspaceId);
    }
}
