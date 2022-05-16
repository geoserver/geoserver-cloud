/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.cluster.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * @since 1.0
 */
@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class DescribeJobsCommand implements CacheJobEvent {

    private @NonNull String instanceId;
    private boolean includeTerminated;

    public DescribeJobsCommand(String instanceId) {
        this(instanceId, false);
    }
}
