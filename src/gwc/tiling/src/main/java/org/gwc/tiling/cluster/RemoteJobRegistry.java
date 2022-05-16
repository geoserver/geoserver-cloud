/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.cluster;

import lombok.NonNull;

import org.gwc.tiling.model.CacheJobStatus;
import org.gwc.tiling.service.CacheJobRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @since 1.0
 */
public class RemoteJobRegistry {

    private ConcurrentMap<String, CacheJobRegistry> remotes = new ConcurrentHashMap<>();

    private @NonNull CacheJobRegistry ofInstance(@NonNull String instanceId) {
        return remotes.computeIfAbsent(instanceId, key -> new CacheJobRegistry());
    }

    public void update(@NonNull String remoteInstanceId, @NonNull CacheJobStatus jobStatus) {
        CacheJobRegistry remoteInstanceRegistry = ofInstance(remoteInstanceId);
        remoteInstanceRegistry.save(jobStatus);
    }
}
