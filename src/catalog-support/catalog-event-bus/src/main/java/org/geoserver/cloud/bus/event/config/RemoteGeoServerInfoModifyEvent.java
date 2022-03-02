/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.geoserver.catalog.plugin.Patch;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@EqualsAndHashCode(callSuper = true)
public class RemoteGeoServerInfoModifyEvent
        extends AbstractRemoteConfigInfoModifyEvent<GeoServerInfo> implements RemoteConfigEvent {
    private static final long serialVersionUID = 1L;

    protected RemoteGeoServerInfoModifyEvent() {
        // default constructor, needed for deserialization
    }

    /**
     * {@code true} only if {@link GeoServerInfo#getUpdateSequence()} is the only modified property.
     */
    private @Getter boolean updateSequenceEvent;

    /**
     * The provided {@link GeoServerInfo}'s {@link GeoServerInfo#getUpdateSequence() update
     * sequence}. Being the most frequently updated property, it's readily available for remote
     * listeners even when the {@link #getPatch() patch} is not sent over the wire.
     */
    private @Getter long updateSequence;

    public RemoteGeoServerInfoModifyEvent(
            GeoServer source,
            GeoServerInfo object,
            Patch patch,
            String originService,
            String destinationService) {
        super(source, object, patch, originService, destinationService);

        this.updateSequenceEvent = patch.size() == 1 && patch.get("updateSequence").isPresent();
        this.updateSequence = object.getUpdateSequence();
    }
}
