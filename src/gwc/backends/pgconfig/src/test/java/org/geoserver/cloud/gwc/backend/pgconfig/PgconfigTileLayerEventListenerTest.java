/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gwc.backend.pgconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.geoserver.catalog.plugin.requestcache.RemoteChangeCounter;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup;
import org.geoserver.cloud.backend.pgconfig.cache.NoopCodec;
import org.geoserver.cloud.gwc.event.EventListenerContext;
import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.GenericApplicationContext;

class PgconfigTileLayerEventListenerTest {

    private static final String LAYER_ID = "layer-id";

    private final EntityCache cache = newEntityCache();
    private final RemoteChangeCounter counter = new RemoteChangeCounter();

    @Test
    void localEventEvictsSettings() {
        putSettings();
        long before = counter.count();

        new PgconfigTileLayerEventListener(cache, counter).onTileLayerEvent(event(false));

        assertThat(settingsCached()).isFalse();
        assertThat(counter.count()).isEqualTo(before);
    }

    @Test
    void remoteEventMovesCounter() {
        putSettings();
        RemoteChangeCounter spied = spy(counter);
        AtomicBoolean goneWhenCounted = new AtomicBoolean();
        doAnswer(invocation -> {
                    goneWhenCounted.set(!settingsCached());
                    return invocation.callRealMethod();
                })
                .when(spied)
                .increment();

        new PgconfigTileLayerEventListener(cache, spied).onTileLayerEvent(event(true));

        assertThat(goneWhenCounted).isTrue();
        assertThat(spied.count()).isOne();
    }

    @Test
    void noCacheOnlyCounts() {
        new PgconfigTileLayerEventListener(null, counter).onTileLayerEvent(event(true));

        assertThat(counter.count()).isOne();
    }

    // a listener of the same event, registered first, finds the settings already evicted
    @Test
    void runsBeforeOtherListeners() {
        putSettings();
        SettingsReader reader = new SettingsReader();
        PgconfigTileLayerEventListener listener = new PgconfigTileLayerEventListener(cache, counter);

        try (GenericApplicationContext context = EventListenerContext.of(reader, listener)) {
            context.publishEvent(event(true));
        }

        assertThat(reader.foundCached).isFalse();
    }

    private class SettingsReader {
        boolean foundCached = true;

        @EventListener(TileLayerEvent.class)
        void on(TileLayerEvent event) {
            foundCached = settingsCached();
        }
    }

    private TileLayerEvent event(boolean remote) {
        TileLayerEvent event = TileLayerEvent.modified(this, LAYER_ID, "ws:roads", null);
        event.setRemote(remote);
        return event;
    }

    private void putSettings() {
        cache.put(CacheKey.tileLayer(LAYER_ID), new byte[] {1}, cache.epoch());
        assertThat(settingsCached()).isTrue();
    }

    private boolean settingsCached() {
        return cache.lookup(CacheKey.tileLayer(LAYER_ID)) instanceof Lookup.Hit;
    }

    private static EntityCache newEntityCache() {
        EntityCache entityCache = new EntityCache("gs-catalog", 1024 * 1024, Duration.ofSeconds(30), new NoopCodec());
        entityCache.setInitialized(true);
        return entityCache;
    }
}
