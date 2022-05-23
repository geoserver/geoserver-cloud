/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event.config;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.bus.event.RemoteModifyEvent;
import org.geoserver.cloud.event.ConfigInfoInfoType;
import org.geoserver.config.GeoServer;

public abstract class AbstractRemoteConfigInfoModifyEvent<I extends Info>
        extends RemoteModifyEvent<GeoServer, I> implements RemoteConfigEvent {
    private static final long serialVersionUID = 1L;

    protected AbstractRemoteConfigInfoModifyEvent() {
        // default constructor, needed for deserialization
    }

    public AbstractRemoteConfigInfoModifyEvent(
            GeoServer source,
            I object,
            Patch patch,
            String originService,
            String destinationService) {
        super(
                source,
                RemoteConfigEvent.resolveId(object),
                ConfigInfoInfoType.valueOf(object),
                patch,
                originService,
                destinationService);
    }
}
