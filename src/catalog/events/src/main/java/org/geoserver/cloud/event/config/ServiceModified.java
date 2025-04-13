/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.config.ServiceInfo;
import org.springframework.core.style.ToStringCreator;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("ServiceModified")
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("serial")
public class ServiceModified extends ConfigInfoModified implements ConfigInfoEvent {

    private @Getter String workspaceId;

    protected ServiceModified() {
        // default constructor, needed for deserialization
    }

    protected ServiceModified(
            long updateSequence, @NonNull ServiceInfo info, @NonNull Patch patch, String workspaceId) {

        super(updateSequence, resolveId(info), prefixedName(info), typeOf(info), patch);
        this.workspaceId = workspaceId;
    }

    @Override
    @NonNull
    public String getObjectName() {
        return Objects.requireNonNull(super.getObjectName());
    }

    protected @Override ToStringCreator toStringBuilder() {
        return super.toStringBuilder().append("workspace", getWorkspaceId());
    }

    public static ServiceModified createLocal(long updateSequence, @NonNull ServiceInfo service, @NonNull Patch patch) {
        final String workspaceId = InfoEvent.resolveNullableId(service.getWorkspace());
        return new ServiceModified(updateSequence, service, patch, workspaceId);
    }
}
