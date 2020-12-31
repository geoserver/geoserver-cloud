/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event.config;

import lombok.Getter;
import lombok.Setter;
import org.geoserver.catalog.Info;
import org.geoserver.cloud.bus.event.RemoteAddEvent;
import org.geoserver.cloud.event.ConfigInfoInfoType;
import org.geoserver.config.GeoServer;

public abstract class AbstractRemoteConfigInfoAddEvent<I extends Info>
        extends RemoteAddEvent<GeoServer, I> implements RemoteConfigEvent {
    private static final long serialVersionUID = 1L;

    private @Getter @Setter I object;

    protected AbstractRemoteConfigInfoAddEvent() {
        // default constructor, needed for deserialization
    }

    public AbstractRemoteConfigInfoAddEvent(
            GeoServer source, I object, String originService, String destinationService) {
        super(
                source,
                RemoteConfigEvent.resolveId(object),
                ConfigInfoInfoType.valueOf(object),
                originService,
                destinationService);
        this.object = object;
    }
}
