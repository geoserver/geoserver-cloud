/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event.config;

import lombok.EqualsAndHashCode;
import org.geoserver.catalog.Info;
import org.geoserver.cloud.bus.event.RemoteAddEvent;
import org.geoserver.cloud.event.ConfigInfoInfoType;
import org.geoserver.config.GeoServer;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@EqualsAndHashCode(callSuper = true)
public class RemoteConfigAddEvent extends RemoteAddEvent<GeoServer, Info>
        implements RemoteConfigEvent {
    private static final long serialVersionUID = 1L;

    private ServiceInfo service;
    private SettingsInfo settings;
    private Info logging;

    protected RemoteConfigAddEvent() {
        // default constructor, needed for deserialization
    }

    public RemoteConfigAddEvent(
            GeoServer source, Info object, String originService, String destinationService) {
        super(
                source,
                RemoteConfigEvent.resolveId(object),
                ConfigInfoInfoType.valueOf(object),
                originService,
                destinationService);
    }

    public @Override Info getObject() {
        if (settings != null) return settings;
        if (service != null) return service;
        if (logging != null) return logging;
        return null;
    }

    public @Override void setObject(Info object) {
        if (object instanceof ServiceInfo) {
            this.service = (ServiceInfo) object;
        } else if (object instanceof SettingsInfo) {
            this.settings = (SettingsInfo) object;
        } else if (object instanceof LoggingInfo) {
            this.logging = object;
        }
    }
}
