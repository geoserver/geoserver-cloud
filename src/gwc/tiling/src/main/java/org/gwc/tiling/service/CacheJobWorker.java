/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.gwc.tiling.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.gwc.tiling.event.JobAborted;
import org.gwc.tiling.model.CacheJobInfo;
import org.springframework.context.ApplicationEventPublisher;

import java.util.concurrent.CompletableFuture;

/**
 * @since 1.0
 */
@RequiredArgsConstructor
public class CacheJobWorker {

    private final @NonNull CacheJobInfo job;
    private final @NonNull ApplicationEventPublisher eventPublisher;
    // private final @NonNull Function<String, TileLayerInfo> tileLayerResolver;

    public @NonNull void launch() {
        // CacheJobRequest request = job.getRequest();
        // CacheIdentifier cacheIdentifier = request.getCacheId();
        // Action action = request.getAction();
        // TilePyramid tiles = request.getTiles();
        // var layer = tileLayerResolver.apply(cacheIdentifier.getLayerName());
        // int metaWidth = layer.getMetaTilingWidth();
        // int metaHeight = layer.getMetaTilingHeight();
        // Stream<TileRange3D> metaTiles = tiles.asMetaTiles(metaWidth, metaHeight);
    }

    public CacheJobInfo abort() {
        CompletableFuture.runAsync(
                () -> {
                    try {
                        Thread.sleep(500);
                        eventPublisher.publishEvent(new JobAborted(job));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
        return job; // throw new UnsupportedOperationException();
    }
}
