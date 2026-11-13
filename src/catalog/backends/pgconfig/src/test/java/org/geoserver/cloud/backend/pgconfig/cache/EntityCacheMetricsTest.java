/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYER;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class EntityCacheMetricsTest {

    private static final Tags TAGS = Tags.of("cache", "gs-catalog", "cache.manager", "pgconfig", "name", "gs-catalog");
    private static final CacheKey LAYER_KEY = CacheKey.entity(LAYER, "layer-1");
    private static final byte[] JSON = "{\"id\": \"layer-1\", \"name\": \"roads\"}".getBytes(UTF_8);

    private final EntityCache cache =
            new EntityCache("gs-catalog", 1024 * 1024, Duration.ofSeconds(30), new NoopCodec());
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @Test
    void metricsRegistered() {
        bindMetrics();

        cache.put(LAYER_KEY, JSON, cache.epoch());
        cache.cleanUp();

        assertThat(registry.find("cache.gets").tags(TAGS).functionCounters()).isNotEmpty();
        Gauge weight = registry.get("cache.weight").tags(TAGS).gauge();
        assertThat(weight.value()).isEqualTo(JSON.length + 64.0);
        assertThat(weight.getId().getBaseUnit()).isEqualTo("bytes");
    }

    // Caffeine records the compute of a store as a load, and Micrometer counts the loads as puts
    @Test
    void countsKeptAndDroppedPuts() {
        bindMetrics();
        long staleEpoch = cache.epoch();
        cache.evict(LAYER_KEY);

        cache.put(LAYER_KEY, JSON, cache.epoch());
        cache.put(LAYER_KEY, JSON, staleEpoch);

        FunctionCounter puts = registry.get("cache.puts").tags(TAGS).functionCounter();
        assertThat(puts.count()).isEqualTo(2);
    }

    @Test
    void noWeightGaugeForCountBound() {
        Cache<String, String> counted =
                Caffeine.newBuilder().maximumSize(10).recordStats().build();
        EntityCacheMetrics metrics = new EntityCacheMetrics("counted", counted);

        metrics.bindTo(registry);

        assertThat(registry.find("cache.gets").tag("name", "counted").functionCounters())
                .isNotEmpty();
        assertThat(registry.find("cache.weight").tag("name", "counted").gauges())
                .isEmpty();
    }

    private void bindMetrics() {
        EntityCacheMetrics metrics = new EntityCacheMetrics(cache);
        metrics.bindTo(registry);
    }
}
