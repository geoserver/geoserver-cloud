/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.security;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

/**
 * A {@link RemoteApplicationEvent} to notify services of changes in the {@link
 * GeoServerSecurityManager security configuration}.
 */
@EqualsAndHashCode(callSuper = true)
public class GeoServerSecurityConfigChangeEvent extends RemoteApplicationEvent {

    private static final long serialVersionUID = 1L;

    private @Getter String reason;

    protected GeoServerSecurityConfigChangeEvent() {
        // default constructor, needed for deserialization
    }

    public GeoServerSecurityConfigChangeEvent(
            Object source, @NonNull String originService, @NonNull String reason) {
        super(source, originService, (String) null);
        this.reason = reason;
    }
}
