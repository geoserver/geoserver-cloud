/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

import org.geoserver.security.GeoServerSecurityManager;

/**
 * Application event to notify services of changes in the {@link GeoServerSecurityManager security
 * configuration}.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "local")
public class SecurityConfigEvent {

    private @NonNull String reason;

    private boolean local;

    public static SecurityConfigEvent createLocal(@NonNull String reason) {
        return new SecurityConfigEvent(reason, true);
    }
}
