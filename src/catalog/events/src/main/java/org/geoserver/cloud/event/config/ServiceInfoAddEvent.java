/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.EqualsAndHashCode;

import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;

/** Event sent when {@link GeoServer#add(org.geoserver.config.ServiceInfo)} is called on a node */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("ServiceInfoAdded")
@EqualsAndHashCode(callSuper = true)
public class ServiceInfoAddEvent extends ConfigInfoAddEvent<ServiceInfoAddEvent, ServiceInfo>
        implements ConfigInfoEvent {

    protected ServiceInfoAddEvent() {
        // default constructor, needed for deserialization
    }

    protected ServiceInfoAddEvent(ServiceInfo object) {
        super(object);
    }

    public static ServiceInfoAddEvent createLocal(ServiceInfo value) {
        return new ServiceInfoAddEvent(value);
    }
}
