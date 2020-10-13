/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@EqualsAndHashCode(callSuper = true)
public class RemoteServiceInfoRemoveEvent extends AbstractRemoteConfigInfoRemoveEvent<ServiceInfo>
        implements RemoteConfigEvent {
    private static final long serialVersionUID = 1L;

    private @Getter @NonNull String workspaceId;

    protected RemoteServiceInfoRemoveEvent() {
        // default constructor, needed for deserialization
    }

    public RemoteServiceInfoRemoveEvent(
            GeoServer source, ServiceInfo object, String originService, String destinationService) {
        super(source, object, originService, destinationService);
        WorkspaceInfo workspace = object.getWorkspace();
        if (workspace != null) {
            this.workspaceId = workspace.getId();
        }
    }
}
