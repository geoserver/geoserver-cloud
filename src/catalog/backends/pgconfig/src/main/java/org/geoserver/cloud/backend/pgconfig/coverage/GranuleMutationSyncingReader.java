/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.coverage;

import java.io.IOException;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.backend.pgconfig.resource.DbBackedFileSynchronizer;
import org.geoserver.security.decorators.DecoratingStructuredGridCoverage2DReader;
import org.geotools.api.data.ResourceInfo;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GranuleStore;
import org.geotools.coverage.grid.io.HarvestedSource;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.util.factory.Hints;

/**
 * {@link StructuredGridCoverage2DReader} decorator used by {@link MosaicSyncingResourcePool} that persists mosaic
 * configuration files after every mutating operation, since gt-imagemosaic rewrites {@code <coverage>.properties} and
 * {@code sample_image.dat} with raw file I/O during harvest and coverage lifecycle changes.
 *
 * @see SyncingGranuleStore
 */
@Slf4j
class GranuleMutationSyncingReader extends DecoratingStructuredGridCoverage2DReader {

    private final StructuredGridCoverage2DReader delegateReader;
    private final String storePath;
    private final DbBackedFileSynchronizer synchronizer;

    public GranuleMutationSyncingReader(
            @NonNull StructuredGridCoverage2DReader delegate,
            @NonNull String storePath,
            @NonNull DbBackedFileSynchronizer synchronizer) {
        super(delegate);
        this.delegateReader = delegate;
        this.storePath = storePath;
        this.synchronizer = synchronizer;
    }

    /**
     * Because {@link DecoratingStructuredGridCoverage2DReader#delegate} is package-private in gs-main and not
     * accessible from this package, these two methods delegate to the constructor-supplied reference instead of
     * {@code super}.
     */
    @Override
    public ServiceInfo getInfo() {
        return delegateReader.getInfo();
    }

    @Override
    public ResourceInfo getInfo(String coverageName) {
        return delegateReader.getInfo(coverageName);
    }

    @Override
    public List<HarvestedSource> harvest(String defaultTargetCoverage, Object source, Hints hints)
            throws IOException, UnsupportedOperationException {
        try {
            return super.harvest(defaultTargetCoverage, source, hints);
        } finally {
            syncQuietly();
        }
    }

    @Override
    public GranuleSource getGranules(String coverageName, boolean readOnly)
            throws IOException, UnsupportedOperationException {
        GranuleSource granules = super.getGranules(coverageName, readOnly);
        if (!readOnly && granules instanceof GranuleStore granuleStore) {
            return new SyncingGranuleStore(granuleStore, storePath, synchronizer);
        }
        return granules;
    }

    @Override
    public void createCoverage(String coverageName, SimpleFeatureType schema)
            throws IOException, UnsupportedOperationException {
        try {
            super.createCoverage(coverageName, schema);
        } finally {
            syncQuietly();
        }
    }

    @Override
    public boolean removeCoverage(String coverageName, boolean delete)
            throws IOException, UnsupportedOperationException {
        try {
            return super.removeCoverage(coverageName, delete);
        } finally {
            syncQuietly();
            syncDeletionsQuietly();
        }
    }

    @Override
    public void delete(boolean deleteData) throws IOException {
        try {
            super.delete(deleteData);
        } finally {
            syncQuietly();
            syncDeletionsQuietly();
        }
    }

    /**
     * Persists mosaic config files, logging and swallowing a push failure instead of rethrowing it, since the coverage
     * operation that triggered the sync already succeeded or failed on its own terms and a sync failure should not mask
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

    /**
     * Removes the database rows of config files the operation deleted locally (coverage removal deletes
     * {@code <coverage>.properties} and {@code sample_image.dat}; store deletion removes the whole directory). Logs and
     * swallows a failure for the same reason as {@link #syncQuietly()}.
     */
    private void syncDeletionsQuietly() {
        try {
            synchronizer.syncDeletions(storePath);
        } catch (RuntimeException e) {
            log.error(
                    "Failed to remove resource store rows for deleted mosaic config files of store path {}",
                    storePath,
                    e);
        }
    }
}
