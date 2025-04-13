/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.security;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.cloud.event.UpdateSequenceEvent;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.core.style.ToStringCreator;

/**
 * Application event to notify services of changes in the {@link GeoServerSecurityManager security
 * configuration}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("SecurityConfigChanged")
@SuppressWarnings("serial")
public class SecurityConfigChanged extends UpdateSequenceEvent {

    private @Getter String reason;

    protected SecurityConfigChanged() {}

    protected SecurityConfigChanged(long updateSequence, @NonNull String reason) {
        super(updateSequence);
        this.reason = reason;
    }

    protected @Override ToStringCreator toStringBuilder() {
        return super.toStringBuilder().append("reason", reason);
    }

    public static SecurityConfigChanged createLocal(long updateSequence, @NonNull String reason) {
        return new SecurityConfigChanged(updateSequence, reason);
    }

    @Override
    public String toShortString() {
        String originService = getOrigin();
        String type = getClass().getSimpleName();
        return "%s[origin: %s, updateSequence: %s, reason: %s]"
                .formatted(type, originService, getUpdateSequence(), getReason());
    }
}
