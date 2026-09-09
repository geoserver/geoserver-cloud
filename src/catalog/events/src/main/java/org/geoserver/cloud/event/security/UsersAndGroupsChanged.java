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
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.springframework.core.style.ToStringCreator;

/**
 * Notifies the other service instances that the users and groups held by a {@link GeoServerUserGroupService} changed.
 *
 * <p>Fired once a {@link GeoServerUserGroupStore} is saved. Receivers reload the service named by {@code serviceName}
 * to refresh their in-memory copy of its contents. Changes to the configuration of the security services themselves are
 * notified through {@link SecurityConfigChanged} instead.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("UsersAndGroupsChanged")
@SuppressWarnings("serial")
public class UsersAndGroupsChanged extends GeoServerEvent {

    private @Getter String serviceName;

    protected UsersAndGroupsChanged() {
        // no-op default constructor for deserialization
    }

    protected UsersAndGroupsChanged(@NonNull String serviceName) {
        super(System.currentTimeMillis(), resolveAuthor());
        this.serviceName = serviceName;
    }

    public static UsersAndGroupsChanged createLocal(@NonNull String serviceName) {
        return new UsersAndGroupsChanged(serviceName);
    }

    protected @Override ToStringCreator toStringBuilder() {
        return super.toStringBuilder().append("serviceName", serviceName);
    }

    @Override
    public String toShortString() {
        return "%s[origin: %s, serviceName: %s]".formatted(getClass().getSimpleName(), getOrigin(), serviceName);
    }
}
