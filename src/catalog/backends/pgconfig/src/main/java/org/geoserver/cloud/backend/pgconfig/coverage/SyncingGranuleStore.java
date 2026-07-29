/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.coverage;

import lombok.NonNull;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.backend.pgconfig.resource.DbBackedFileSynchronizer;
import org.geotools.api.filter.Filter;
import org.geotools.coverage.grid.io.GranuleStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.util.factory.Hints;

/**
 * {@link GranuleStore} decorator persisting mosaic configuration files after granule mutations (REST granule deletion
 * goes through {@link GranuleStore#removeGranules}).
 */
@Slf4j
class SyncingGranuleStore implements GranuleStore {

    @Delegate
    private final GranuleStore delegate;

    private final String storePath;
    private final DbBackedFileSynchronizer synchronizer;

    public SyncingGranuleStore(
            @NonNull GranuleStore delegate, @NonNull String storePath, @NonNull DbBackedFileSynchronizer synchronizer) {
        this.delegate = delegate;
        this.storePath = storePath;
        this.synchronizer = synchronizer;
    }

    @Override
    public void addGranules(SimpleFeatureCollection granules) {
        try {
            delegate.addGranules(granules);
        } finally {
            syncQuietly();
        }
    }

    @Override
    public int removeGranules(Filter filter) {
        try {
            return delegate.removeGranules(filter);
        } finally {
            syncQuietly();
        }
    }

    @Override
    public int removeGranules(Filter filter, Hints hints) {
        try {
            return delegate.removeGranules(filter, hints);
        } finally {
            syncQuietly();
        }
    }

    @Override
    public void updateGranules(String[] attributeNames, Object[] attributeValues, Filter filter) {
        try {
            delegate.updateGranules(attributeNames, attributeValues, filter);
        } finally {
            syncQuietly();
        }
    }

    /**
     * Persists mosaic config files, logging and swallowing a push failure instead of rethrowing it, since the granule
     * mutation that triggered the sync already succeeded or failed on its own terms and a sync failure should not mask
     * that outcome. A failed push self-heals on the next sync trigger, as {@link DbBackedFileSynchronizer} only
     * advances its newest-synced mtime threshold on success.
     */
    private void syncQuietly() {
        try {
            synchronizer.sync(storePath);
        } catch (RuntimeException e) {
            log.error("Failed to persist mosaic config files for store path {}", storePath, e);
        }
    }
}
