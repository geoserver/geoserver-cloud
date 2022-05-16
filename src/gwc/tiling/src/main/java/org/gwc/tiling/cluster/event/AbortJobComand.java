/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.cluster.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

/**
 * @since 1.0
 */
@Value
@With
@Builder
@Jacksonized
@AllArgsConstructor
public class AbortJobComand implements CacheJobEvent {

    private @NonNull String instanceId;
    private String jobId;

    public AbortJobComand(String instanceId) {
        this(instanceId, null);
    }
}
