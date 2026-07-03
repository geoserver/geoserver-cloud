/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.coverage;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.cloud.backend.pgconfig.resource.DbBackedFileSynchronizer;
import org.geoserver.platform.resource.Paths;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.util.factory.Hints;

/**
 * {@link ResourcePool} decorating {@link #getGridCoverageReader} of resource-store-resident stores (relative
 * {@code file:data/...} URLs): materializes the store's mosaic config files into the local cache before reader
 * creation, and persists them back right after reader creation and after every granule mutation, making REST-created
 * ImageMosaics work across pods on the pgconfig backend. Absolute store URLs (e.g. a shared volume) are returned
 * untouched.
 *
 * <p>The pre-read materialization matters because upstream {@link ResourcePool} resolves the store URL with raw file
 * I/O against the resource loader's base directory instead of through the Resource API, bypassing the pgconfig resource
 * store's own on-demand materialization. This class and its reader decorators are the mosaic-specific trigger points of
 * the general-purpose {@link DbBackedFileSynchronizer}.
 */
@Slf4j
public class MosaicSyncingResourcePool extends ResourcePool {

    private final DbBackedFileSynchronizer synchronizer;

    public MosaicSyncingResourcePool(@NonNull Catalog catalog, @NonNull DbBackedFileSynchronizer synchronizer) {
        super(catalog);
        this.synchronizer = synchronizer;
    }

    /**
     * Returns the resource store path of the coverage store when its URL is relative (lives inside the resource store),
     * empty for null stores, absolute paths, and non-file URLs.
     */
    static Optional<String> resourceStorePath(CoverageStoreInfo storeInfo) {
        String url = storeInfo == null ? null : storeInfo.getURL();
        if (url == null || url.contains("://")) {
            return Optional.empty();
        }
        String path = url.startsWith("file:") ? url.substring("file:".length()) : url;
        if (path.isEmpty() || path.startsWith("/")) {
            return Optional.empty();
        }
        return Optional.of(Paths.valid(path));
    }

    @Override
    public GridCoverageReader getGridCoverageReader(CoverageInfo info, String coverageName, Hints hints)
            throws IOException {
        CoverageStoreInfo storeInfo = info.getStore();
        materializeQuietly(storeInfo);
        GridCoverageReader reader = super.getGridCoverageReader(info, coverageName, hints);
        return decorate(storeInfo, reader);
    }

    @Override
    public GridCoverageReader getGridCoverageReader(CoverageStoreInfo storeInfo, String coverageName, Hints hints)
            throws IOException {
        materializeQuietly(storeInfo);
        GridCoverageReader reader = super.getGridCoverageReader(storeInfo, coverageName, hints);
        return decorate(storeInfo, reader);
    }

    /**
     * Materializes the store's mosaic config files into the local cache before upstream {@link ResourcePool} resolves
     * the reader, logging and swallowing a materialization failure instead of rethrowing it, since a materialization
     * failure must not fail reader acquisition; upstream's own failure will tell the story if materialization was the
     * actual problem.
     */
    @VisibleForTesting
    void materializeQuietly(CoverageStoreInfo storeInfo) {
        Optional<String> storePath = resourceStorePath(storeInfo);
        if (storePath.isEmpty()) {
            return;
        }
        try {
            synchronizer.materialize(storePath.orElseThrow());
        } catch (RuntimeException e) {
            log.error("Failed to materialize mosaic config files for store {}", storeInfo.getName(), e);
        }
    }

    @VisibleForTesting
    GridCoverageReader decorate(CoverageStoreInfo storeInfo, GridCoverageReader reader) {
        Optional<String> storePath = resourceStorePath(storeInfo);
        if (storePath.isPresent() && reader instanceof StructuredGridCoverage2DReader structured) {
            String path = storePath.orElseThrow();
            syncQuietly(storeInfo, path);
            return new GranuleMutationSyncingReader(structured, path, synchronizer);
        }
        return reader;
    }

    /**
     * Persists mosaic config files right after reader acquisition, throttled: reader acquisition runs on every WMS/WCS
     * request, and an unthrottled sync would scan the store directory per request even when nothing was written.
     * Mutating operations propagate immediately through {@link GranuleMutationSyncingReader}'s unthrottled syncs. Logs
     * and swallows a push failure instead of rethrowing it, since a sync failure must not fail reader acquisition. A
     * failed push self-heals on the next sync trigger, as {@link DbBackedFileSynchronizer} only advances its
     * newest-synced mtime threshold and throttle stamp on success.
     */
    private void syncQuietly(CoverageStoreInfo storeInfo, String storePath) {
        try {
            synchronizer.syncThrottled(storePath);
        } catch (RuntimeException e) {
            log.error("Failed to persist mosaic config files for store {}", storeInfo.getName(), e);
        }
    }
}
