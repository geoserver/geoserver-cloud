/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event.config;

import lombok.EqualsAndHashCode;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Remote event sent when {@link GeoServer#add(org.geoserver.config.ServiceInfo)} is called on a
 * node
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@EqualsAndHashCode(callSuper = true)
public class RemoteServiceInfoAddEvent extends AbstractRemoteConfigInfoAddEvent<ServiceInfo>
        implements RemoteConfigEvent {
    private static final long serialVersionUID = 1L;

    protected RemoteServiceInfoAddEvent() {
        // default constructor, needed for deserialization
    }

    public RemoteServiceInfoAddEvent(
            GeoServer source, ServiceInfo object, String originService, String destinationService) {
        super(source, object, originService, destinationService);
    }
}
