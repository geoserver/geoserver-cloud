/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event.config;

import lombok.EqualsAndHashCode;

import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Remote event sent when {@link GeoServer#setGlobal(GeoServerInfo)} is called on a node */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@EqualsAndHashCode(callSuper = true)
public class RemoteGeoServerInfoSetEvent extends AbstractRemoteConfigInfoAddEvent<GeoServerInfo>
        implements RemoteConfigEvent {
    private static final long serialVersionUID = 1L;

    protected RemoteGeoServerInfoSetEvent() {
        // default constructor, needed for deserialization
    }

    public RemoteGeoServerInfoSetEvent(
            GeoServer source,
            GeoServerInfo object,
            String originService,
            String destinationService) {
        super(source, object, originService, destinationService);
    }
}
