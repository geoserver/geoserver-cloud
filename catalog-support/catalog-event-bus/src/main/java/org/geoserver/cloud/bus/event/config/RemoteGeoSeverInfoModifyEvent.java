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
public class RemoteGeoSeverInfoModifyEvent extends RemoteConfigModifyEvent<GeoServerInfo>
        implements RemoteConfigEvent {
    private static final long serialVersionUID = 1L;

    protected RemoteGeoSeverInfoModifyEvent() {
        // default constructor, needed for deserialization
    }

    private @Getter boolean updateSequenceEvent;

    public RemoteGeoSeverInfoModifyEvent(
            GeoServer source,
            GeoServerInfo object,
            Patch patch,
            String originService,
            String destinationService) {
        super(source, object, patch, originService, destinationService);

        this.updateSequenceEvent = patch.get("updateSequence").isPresent();
    }
}
