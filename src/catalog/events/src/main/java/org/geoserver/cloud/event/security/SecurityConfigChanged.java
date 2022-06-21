/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.security;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import org.geoserver.cloud.event.UpdateSequenceEvent;
import org.geoserver.security.GeoServerSecurityManager;

/**
 * Application event to notify services of changes in the {@link GeoServerSecurityManager security
 * configuration}.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("SecurityConfigChanged")
public class SecurityConfigChanged extends UpdateSequenceEvent<SecurityConfigChanged> {

    private @NonNull String reason;

    protected SecurityConfigChanged() {}

    protected SecurityConfigChanged(@NonNull Long updateSequence, @NonNull String reason) {
        super(updateSequence);
        this.reason = reason;
    }

    public static SecurityConfigChanged createLocal(
            @NonNull Long updateSequence, @NonNull String reason) {
        return new SecurityConfigChanged(updateSequence, reason);
    }
}
