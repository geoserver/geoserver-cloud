/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.security;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.cloud.event.GeoServerEvent;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.springframework.core.style.ToStringCreator;

/**
 * Notifies the other service instances that the roles held by a {@link GeoServerRoleService} changed.
 *
 * <p>Fired once a {@link GeoServerRoleStore} is saved. Receivers reload the service named by {@code serviceName} to
 * refresh their in-memory copy of its contents. Changes to the configuration of the security services themselves are
 * notified through {@link SecurityConfigChanged} instead.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("RolesChanged")
@SuppressWarnings("serial")
public class RolesChanged extends GeoServerEvent {

    private @Getter String serviceName;

    protected RolesChanged() {
        // no-op default constructor for deserialization
    }

    protected RolesChanged(@NonNull String serviceName) {
        super(System.currentTimeMillis(), resolveAuthor());
        this.serviceName = serviceName;
    }

    public static RolesChanged createLocal(@NonNull String serviceName) {
        return new RolesChanged(serviceName);
    }

    protected @Override ToStringCreator toStringBuilder() {
        return super.toStringBuilder().append("serviceName", serviceName);
    }

    @Override
    public String toShortString() {
        return "%s[origin: %s, serviceName: %s]".formatted(getClass().getSimpleName(), getOrigin(), serviceName);
    }
}
