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

import org.gwc.tiling.model.CacheJobStatus;

import java.util.List;

/**
 * @since 1.0
 */
@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class DescribeJobsResponse implements CacheJobEvent {

    private @NonNull String instanceId;

    /** If {@code null}, send the event to all instances, otherwise just to the one with this id */
    private String targetInstanceId;

    private @NonNull List<CacheJobStatus> jobs;
}
