/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event.config;

import lombok.EqualsAndHashCode;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@EqualsAndHashCode(callSuper = true)
public class RemoteSettingsInfoModifyEvent extends RemoteConfigModifyEvent<SettingsInfo>
        implements RemoteConfigEvent {
    private static final long serialVersionUID = 1L;

    protected RemoteSettingsInfoModifyEvent() {
        // default constructor, needed for deserialization
    }

    public RemoteSettingsInfoModifyEvent(
            GeoServer source,
            SettingsInfo object,
            Patch patch,
            String originService,
            String destinationService) {
        super(source, object, patch, originService, destinationService);
    }
}
