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
@JsonTypeName("ServiceAdded")
@EqualsAndHashCode(callSuper = true)
public class ServiceAdded extends ConfigInfoAdded<ServiceAdded, ServiceInfo>
        implements ConfigInfoEvent {

    protected ServiceAdded() {
        // default constructor, needed for deserialization
    }

    protected ServiceAdded(long updateSequence, ServiceInfo object) {
        super(updateSequence, object);
    }

    public static ServiceAdded createLocal(long updateSequence, ServiceInfo value) {
        return new ServiceAdded(updateSequence, value);
    }
}
