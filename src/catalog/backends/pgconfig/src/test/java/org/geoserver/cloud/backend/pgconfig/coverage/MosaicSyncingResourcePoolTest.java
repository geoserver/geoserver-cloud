/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.coverage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.cloud.backend.pgconfig.resource.DbBackedFileSynchronizer;
import org.geotools.api.coverage.grid.GridCoverageReader;
import org.geotools.api.filter.Filter;
import org.geotools.coverage.grid.io.GranuleStore;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.util.factory.Hints;
import org.junit.jupiter.api.Test;

class MosaicSyncingResourcePoolTest {

    private static final String STORE_PATH = "data/ws/store";

    private CoverageStoreInfo mosaicStore(String url) {
        CoverageStoreInfo info = mock(CoverageStoreInfo.class);
        when(info.getURL()).thenReturn(url);
        return info;
    }

    @Test
    void resourceStorePathResolvesRelativeFileUrlsOnly() {
        assertThat(MosaicSyncingResourcePool.resourceStorePath(mosaicStore("file:data/ws/store")))
                .contains("data/ws/store");
        assertThat(MosaicSyncingResourcePool.resourceStorePath(mosaicStore("data/ws/store")))
                .contains("data/ws/store");
        assertThat(MosaicSyncingResourcePool.resourceStorePath(mosaicStore("file:///abs/path")))
                .isEmpty();
        assertThat(MosaicSyncingResourcePool.resourceStorePath(mosaicStore("/abs/path")))
                .isEmpty();
        assertThat(MosaicSyncingResourcePool.resourceStorePath(mosaicStore("http://example.com/x")))
                .isEmpty();
        assertThat(MosaicSyncingResourcePool.resourceStorePath(mosaicStore(null)))
                .isEmpty();
        assertThat(MosaicSyncingResourcePool.resourceStorePath(null)).isEmpty();
    }

    @Test
    void readerDecoratorSyncsAfterHarvest() throws Exception {
        StructuredGridCoverage2DReader delegate = mock(StructuredGridCoverage2DReader.class);
        DbBackedFileSynchronizer synchronizer = mock(DbBackedFileSynchronizer.class);

        GranuleMutationSyncingReader reader = new GranuleMutationSyncingReader(delegate, STORE_PATH, synchronizer);
        reader.harvest("cov", "http://example.com/granule.tif", new Hints());

        verify(delegate).harvest("cov", "http://example.com/granule.tif", new Hints());
        verify(synchronizer).sync(STORE_PATH);
    }

    @Test
    void granuleStoreDecoratorSyncsAfterRemoval() throws Exception {
        StructuredGridCoverage2DReader delegate = mock(StructuredGridCoverage2DReader.class);
        GranuleStore granules = mock(GranuleStore.class);
        when(delegate.getGranules("cov", false)).thenReturn(granules);
        DbBackedFileSynchronizer synchronizer = mock(DbBackedFileSynchronizer.class);

        GranuleMutationSyncingReader reader = new GranuleMutationSyncingReader(delegate, STORE_PATH, synchronizer);
        GranuleStore decorated = (GranuleStore) reader.getGranules("cov", false);
        decorated.removeGranules(Filter.INCLUDE);

        verify(granules).removeGranules(Filter.INCLUDE);
        verify(synchronizer).sync(STORE_PATH);
    }

    @Test
    void readOnlyGranulesAreNotWrapped() throws Exception {
        StructuredGridCoverage2DReader delegate = mock(StructuredGridCoverage2DReader.class);
        org.geotools.coverage.grid.io.GranuleSource source = mock(org.geotools.coverage.grid.io.GranuleSource.class);
        when(delegate.getGranules("cov", true)).thenReturn(source);

        GranuleMutationSyncingReader reader =
                new GranuleMutationSyncingReader(delegate, STORE_PATH, mock(DbBackedFileSynchronizer.class));

        assertThat(reader.getGranules("cov", true)).isSameAs(source);
    }

    @Test
    void harvestFailurePropagatesDelegateExceptionAndStillSyncs() throws Exception {
        StructuredGridCoverage2DReader delegate = mock(StructuredGridCoverage2DReader.class);
        DbBackedFileSynchronizer synchronizer = mock(DbBackedFileSynchronizer.class);
        IOException failure = new IOException("boom");
        when(delegate.harvest(any(), any(), any())).thenThrow(failure);

        GranuleMutationSyncingReader reader = new GranuleMutationSyncingReader(delegate, STORE_PATH, synchronizer);

        assertThatThrownBy(() -> reader.harvest("cov", "http://example.com/granule.tif", new Hints()))
                .isSameAs(failure);

        verify(synchronizer).sync(STORE_PATH);
    }

    @Test
    void harvestSucceedsWhenSyncFails() {
        StructuredGridCoverage2DReader delegate = mock(StructuredGridCoverage2DReader.class);
        DbBackedFileSynchronizer synchronizer = mock(DbBackedFileSynchronizer.class);
        doThrow(new UncheckedIOException("push failed", new IOException()))
                .when(synchronizer)
                .sync(STORE_PATH);

        GranuleMutationSyncingReader reader = new GranuleMutationSyncingReader(delegate, STORE_PATH, synchronizer);

        assertThatCode(() -> reader.harvest("cov", "http://example.com/granule.tif", new Hints()))
                .doesNotThrowAnyException();

        verify(synchronizer).sync(STORE_PATH);
    }

    @Test
    void granuleRemovalSucceedsWhenSyncFails() throws Exception {
        StructuredGridCoverage2DReader delegate = mock(StructuredGridCoverage2DReader.class);
        GranuleStore granules = mock(GranuleStore.class);
        when(delegate.getGranules("cov", false)).thenReturn(granules);
        DbBackedFileSynchronizer synchronizer = mock(DbBackedFileSynchronizer.class);
        doThrow(new UncheckedIOException("push failed", new IOException()))
                .when(synchronizer)
                .sync(STORE_PATH);

        GranuleMutationSyncingReader reader = new GranuleMutationSyncingReader(delegate, STORE_PATH, synchronizer);
        GranuleStore decorated = (GranuleStore) reader.getGranules("cov", false);

        assertThatCode(() -> decorated.removeGranules(Filter.INCLUDE)).doesNotThrowAnyException();

        verify(granules).removeGranules(Filter.INCLUDE);
        verify(synchronizer).sync(STORE_PATH);
    }

    @Test
    void decorateWrapsStructuredReaderForResourceStoreResidentStore() {
        CoverageStoreInfo store = mosaicStore("file:data/ws/store");
        DbBackedFileSynchronizer synchronizer = mock(DbBackedFileSynchronizer.class);
        MosaicSyncingResourcePool pool = new MosaicSyncingResourcePool(mock(Catalog.class), synchronizer);
        StructuredGridCoverage2DReader reader = mock(StructuredGridCoverage2DReader.class);

        GridCoverageReader decorated = pool.decorate(store, reader);

        assertThat(decorated).isInstanceOf(GranuleMutationSyncingReader.class);
        verify(synchronizer).syncThrottled(STORE_PATH);
    }

    @Test
    void decorateReturnsSameReaderForAbsoluteStoreUrl() {
        CoverageStoreInfo store = mosaicStore("/abs/path");
        DbBackedFileSynchronizer synchronizer = mock(DbBackedFileSynchronizer.class);
        MosaicSyncingResourcePool pool = new MosaicSyncingResourcePool(mock(Catalog.class), synchronizer);
        StructuredGridCoverage2DReader reader = mock(StructuredGridCoverage2DReader.class);

        GridCoverageReader decorated = pool.decorate(store, reader);

        assertThat(decorated).isSameAs(reader);
        verify(synchronizer, never()).syncThrottled(anyString());
    }

    @Test
    void decorateReturnsSameReaderForNonStructuredReader() {
        CoverageStoreInfo store = mosaicStore("file:data/ws/store");
        DbBackedFileSynchronizer synchronizer = mock(DbBackedFileSynchronizer.class);
        MosaicSyncingResourcePool pool = new MosaicSyncingResourcePool(mock(Catalog.class), synchronizer);
        GridCoverageReader reader = mock(GridCoverageReader.class);

        GridCoverageReader decorated = pool.decorate(store, reader);

        assertThat(decorated).isSameAs(reader);
        verify(synchronizer, never()).syncThrottled(anyString());
    }

    @Test
    void decorateReturnsSameReaderForNullStoreUrl() {
        CoverageStoreInfo store = mosaicStore(null);
        DbBackedFileSynchronizer synchronizer = mock(DbBackedFileSynchronizer.class);
        MosaicSyncingResourcePool pool = new MosaicSyncingResourcePool(mock(Catalog.class), synchronizer);
        StructuredGridCoverage2DReader reader = mock(StructuredGridCoverage2DReader.class);

        GridCoverageReader decorated = pool.decorate(store, reader);

        assertThat(decorated).isSameAs(reader);
        verify(synchronizer, never()).syncThrottled(anyString());
    }

    @Test
    void removeCoverageSyncsDeletions() throws Exception {
        StructuredGridCoverage2DReader delegate = mock(StructuredGridCoverage2DReader.class);
        DbBackedFileSynchronizer synchronizer = mock(DbBackedFileSynchronizer.class);

        GranuleMutationSyncingReader reader = new GranuleMutationSyncingReader(delegate, STORE_PATH, synchronizer);
        reader.removeCoverage("cov", true);

        verify(delegate).removeCoverage("cov", true);
        verify(synchronizer).sync(STORE_PATH);
        verify(synchronizer).syncDeletions(STORE_PATH);
    }

    @Test
    void readerDeleteSyncsDeletions() throws Exception {
        StructuredGridCoverage2DReader delegate = mock(StructuredGridCoverage2DReader.class);
        DbBackedFileSynchronizer synchronizer = mock(DbBackedFileSynchronizer.class);

        GranuleMutationSyncingReader reader = new GranuleMutationSyncingReader(delegate, STORE_PATH, synchronizer);
        reader.delete(true);

        verify(delegate).delete(true);
        verify(synchronizer).sync(STORE_PATH);
        verify(synchronizer).syncDeletions(STORE_PATH);
    }

    @Test
    void readerDeleteSucceedsWhenDeletionSyncFails() {
        StructuredGridCoverage2DReader delegate = mock(StructuredGridCoverage2DReader.class);
        DbBackedFileSynchronizer synchronizer = mock(DbBackedFileSynchronizer.class);
        doThrow(new UncheckedIOException("row delete failed", new IOException()))
                .when(synchronizer)
                .syncDeletions(STORE_PATH);

        GranuleMutationSyncingReader reader = new GranuleMutationSyncingReader(delegate, STORE_PATH, synchronizer);

        assertThatCode(() -> reader.delete(true)).doesNotThrowAnyException();

        verify(synchronizer).syncDeletions(STORE_PATH);
    }

    @Test
    void materializeQuietlyMaterializesResourceStoreResidentStore() {
        CoverageStoreInfo store = mosaicStore("file:data/ws/store");
        DbBackedFileSynchronizer synchronizer = mock(DbBackedFileSynchronizer.class);
        MosaicSyncingResourcePool pool = new MosaicSyncingResourcePool(mock(Catalog.class), synchronizer);

        pool.materializeQuietly(store);

        verify(synchronizer).materialize(STORE_PATH);
    }

    @Test
    void materializeQuietlySkipsAbsoluteStoreUrl() {
        CoverageStoreInfo store = mosaicStore("/abs/path");
        DbBackedFileSynchronizer synchronizer = mock(DbBackedFileSynchronizer.class);
        MosaicSyncingResourcePool pool = new MosaicSyncingResourcePool(mock(Catalog.class), synchronizer);

        pool.materializeQuietly(store);

        verify(synchronizer, never()).materialize(anyString());
    }

    @Test
    void materializeQuietlySkipsNullStore() {
        DbBackedFileSynchronizer synchronizer = mock(DbBackedFileSynchronizer.class);
        MosaicSyncingResourcePool pool = new MosaicSyncingResourcePool(mock(Catalog.class), synchronizer);

        pool.materializeQuietly(null);

        verify(synchronizer, never()).materialize(anyString());
    }

    @Test
    void materializeQuietlyDoesNotThrowWhenMaterializationFails() {
        CoverageStoreInfo store = mosaicStore("file:data/ws/store");
        when(store.getName()).thenReturn("mosaic");
        DbBackedFileSynchronizer synchronizer = mock(DbBackedFileSynchronizer.class);
        doThrow(new UncheckedIOException("materialize failed", new IOException()))
                .when(synchronizer)
                .materialize(STORE_PATH);
        MosaicSyncingResourcePool pool = new MosaicSyncingResourcePool(mock(Catalog.class), synchronizer);

        assertThatCode(() -> pool.materializeQuietly(store)).doesNotThrowAnyException();

        verify(synchronizer).materialize(STORE_PATH);
    }
}
