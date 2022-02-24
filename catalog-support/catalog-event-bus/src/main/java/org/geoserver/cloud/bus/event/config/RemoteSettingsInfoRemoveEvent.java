/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@EqualsAndHashCode(callSuper = true)
public class RemoteSettingsInfoRemoveEvent extends AbstractRemoteConfigInfoRemoveEvent<SettingsInfo>
        implements RemoteConfigEvent {
    private static final long serialVersionUID = 1L;

    private @Getter @NonNull String workspaceId;

    protected RemoteSettingsInfoRemoveEvent() {
        // default constructor, needed for deserialization
    }

    public RemoteSettingsInfoRemoveEvent(
            GeoServer source,
            SettingsInfo object,
            String originService,
            String destinationService) {
        super(source, object, originService, destinationService);
        this.workspaceId = object.getWorkspace().getId();
    }
}
