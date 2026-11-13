/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.config.catalog.backend.pgconfig;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.geoserver.catalog.plugin.requestcache.RequestCache;
import org.geoserver.catalog.plugin.requestcache.RequestCacheScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RequestCacheMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private final RequestCacheMetrics metrics = new RequestCacheMetrics();

    @AfterEach
    void closeScope() {
        RequestCacheScope.close();
        metrics.close();
    }

    // a cache of one entry: the second put is rejected
    @Test
    void closedScopeAddsCounts() {
        metrics.bindTo(registry);
        RequestCacheScope.open(1);
        RequestCache cache = RequestCacheScope.current();
        cache.put("roads", "the roads layer");
        cache.put("rivers", "the rivers layer");
        cache.found("roads");
        cache.found("lakes");
        cache.found("rivers");

        RequestCacheScope.close();

        assertThat(lookups("hit")).isEqualTo(1);
        assertThat(lookups("miss")).isEqualTo(2);
        assertThat(rejected()).isEqualTo(1);
    }

    @Test
    void openScopeAddsNothing() {
        metrics.bindTo(registry);
        RequestCacheScope.open(10);
        RequestCache cache = RequestCacheScope.current();

        cache.found("roads");

        assertThat(lookups("miss")).isZero();
    }

    @Test
    void closedMetricsStopCounting() {
        metrics.bindTo(registry);
        metrics.close();
        RequestCacheScope.open(10);
        RequestCache cache = RequestCacheScope.current();
        cache.found("roads");

        RequestCacheScope.close();

        assertThat(lookups("miss")).isZero();
    }

    @Test
    void gaugeShowsBudgetInUse() {
        metrics.bindTo(registry);
        RequestCacheScope.open(10);
        RequestCache cache = RequestCacheScope.current();
        double before = entries();

        cache.put("roads", "the roads layer");
        cache.put("rivers", "the rivers layer");

        assertThat(entries()).isEqualTo(before + 2);
    }

    private double entries() {
        return registry.get("catalog.request.cache.entries").gauge().value();
    }

    private double lookups(String result) {
        Counter lookups = registry.get("catalog.request.cache.lookups")
                .tag("result", result)
                .counter();
        return lookups.count();
    }

    private double rejected() {
        Counter rejected = registry.get("catalog.request.cache.rejected").counter();
        return rejected.count();
    }
}
