/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gwc.backend.pgconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.geoserver.catalog.plugin.requestcache.RequestCache;
import org.geoserver.catalog.plugin.requestcache.RequestCacheScope;
import org.geoserver.cloud.gwc.backend.pgconfig.RequestCachingTileLayerInfoRepository.SettingsKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RequestCachingTileLayerInfoRepositoryTest {

    private static final int REQUEST_CACHE_SIZE = 100;

    private TileLayerInfoRepository delegate;

    private RequestCachingTileLayerInfoRepository repository;

    @BeforeEach
    void setUp() {
        delegate = mock(TileLayerInfoRepository.class);
        repository = new RequestCachingTileLayerInfoRepository(delegate);
    }

    @AfterEach
    void endRequest() {
        RequestCacheScope.close();
    }

    @Test
    void settingsOncePerRequest() {
        TileLayerInfo settings = new TileLayerInfo().setEnabled(true);
        when(delegate.findSettings("l1")).thenReturn(Optional.of(settings));
        RequestCacheScope.open(REQUEST_CACHE_SIZE);

        Optional<TileLayerInfo> first = repository.findSettings("l1");
        Optional<TileLayerInfo> second = repository.findSettings("l1");

        assertThat(first).containsSame(settings);
        assertThat(second).containsSame(settings);
        verify(delegate, times(1)).findSettings("l1");
    }

    @Test
    void noneKeptAsMiss() {
        when(delegate.findSettings("l1")).thenReturn(Optional.empty());
        RequestCacheScope.open(REQUEST_CACHE_SIZE);

        Optional<TileLayerInfo> first = repository.findSettings("l1");
        Optional<TileLayerInfo> second = repository.findSettings("l1");

        assertThat(first).isEmpty();
        assertThat(second).isEmpty();
        verify(delegate, times(1)).findSettings("l1");
        RequestCache requestCache = RequestCacheScope.current();
        assertThat(requestCache.found(new SettingsKey("l1"))).containsSame(RequestCache.MISS);
    }

    // a request reads its own writes
    @Test
    void writeEvictsRequestKey() {
        TileLayerInfo settings = new TileLayerInfo();
        when(delegate.findSettings("l1")).thenReturn(Optional.empty(), Optional.of(settings), Optional.empty());
        RequestCacheScope.open(REQUEST_CACHE_SIZE);

        assertThat(repository.findSettings("l1")).isEmpty();
        repository.save("l1", settings);
        assertThat(repository.findSettings("l1")).containsSame(settings);
        repository.delete("l1");
        assertThat(repository.findSettings("l1")).isEmpty();

        verify(delegate).save("l1", settings);
        verify(delegate).delete("l1");
        verify(delegate, times(3)).findSettings("l1");
    }

    // the lookups by id of the request find the settings of the listing, and its answer "none"
    @Test
    void listedSettingsOncePerRequest() {
        TileLayerInfo settings = new TileLayerInfo().setEnabled(true);
        List<String> ids = List.of("l1", "l2");
        when(delegate.findAllSettings(ids)).thenReturn(Map.of("l1", settings));
        RequestCacheScope.open(REQUEST_CACHE_SIZE);

        Map<String, TileLayerInfo> first = repository.findAllSettings(ids);
        Map<String, TileLayerInfo> second = repository.findAllSettings(ids);

        assertThat(first).containsOnly(entry("l1", settings));
        assertThat(second).containsOnly(entry("l1", settings));
        assertThat(repository.findSettings("l1")).containsSame(settings);
        assertThat(repository.findSettings("l2")).isEmpty();
        verify(delegate, times(1)).findAllSettings(ids);
        verify(delegate, never()).findSettings(any());
    }

    @Test
    void listingAsksUncachedIdsOnly() {
        TileLayerInfo l1 = new TileLayerInfo();
        TileLayerInfo l2 = new TileLayerInfo().setEnabled(true);
        when(delegate.findSettings("l1")).thenReturn(Optional.of(l1));
        when(delegate.findAllSettings(List.of("l2"))).thenReturn(Map.of("l2", l2));
        RequestCacheScope.open(REQUEST_CACHE_SIZE);

        repository.findSettings("l1");
        Map<String, TileLayerInfo> listed = repository.findAllSettings(List.of("l1", "l2"));

        assertThat(listed).containsOnly(entry("l1", l1), entry("l2", l2));
        verify(delegate, times(1)).findAllSettings(List.of("l2"));
    }

    @Test
    void noScopeDelegates() {
        TileLayerInfo settings = new TileLayerInfo();
        when(delegate.findSettings("l1")).thenReturn(Optional.of(settings));

        repository.findSettings("l1");
        Optional<TileLayerInfo> second = repository.findSettings("l1");

        assertThat(second).containsSame(settings);
        verify(delegate, times(2)).findSettings("l1");
    }
}
