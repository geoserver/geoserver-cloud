/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.config.catalog.backend.pgconfig;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.geoserver.catalog.plugin.requestcache.RequestCache;
import org.geoserver.catalog.plugin.requestcache.RequestCacheBudget;
import org.geoserver.catalog.plugin.requestcache.RequestCacheScope;

/**
 * Registers the counts of the request caches with Micrometer: {@code catalog.request.cache.lookups}, tagged
 * {@code result=hit} for the lookups answered by the cache of a request and {@code result=miss} for the others, and
 * {@code catalog.request.cache.rejected} for the objects not added to a full or closed cache, or past the budget of the
 * request caches. The gauge {@code catalog.request.cache.entries} is the number of entries held by the live request
 * caches, against their budget, see {@link RequestCacheBudget#shared()}.
 *
 * <p>The counts of a cache are added when its scope closes, see {@link RequestCacheScope#onClose}: a request adds its
 * counts when it ends. This binder keeps Micrometer types out of the classes of the cache, as
 * {@code EntityCacheMetrics} does for the entity cache.
 */
public class RequestCacheMetrics implements MeterBinder, AutoCloseable {

    private final List<Runnable> listenerRemovals = new CopyOnWriteArrayList<>();

    @Override
    public void bindTo(MeterRegistry registry) {
        Counter hits = lookups(registry, "hit");
        Counter misses = lookups(registry, "miss");
        Counter rejected = Counter.builder("catalog.request.cache.rejected")
                .description("Objects not added to the cache of a request, because the cache was full or closed, "
                        + "or the request caches held their budget of entries")
                .register(registry);
        Runnable removal = RequestCacheScope.onClose(cache -> addCounts(cache, hits, misses, rejected));
        listenerRemovals.add(removal);
        Gauge.builder("catalog.request.cache.entries", RequestCacheBudget.shared(), RequestCacheBudget::used)
                .description("Entries held by the live request caches, against their budget of entries")
                .register(registry);
    }

    private static Counter lookups(MeterRegistry registry, String result) {
        return Counter.builder("catalog.request.cache.lookups")
                .description("Lookups of catalog and configuration objects in the cache of a request")
                .tag("result", result)
                .register(registry);
    }

    private static void addCounts(RequestCache cache, Counter hits, Counter misses, Counter rejected) {
        hits.increment(cache.hits());
        misses.increment(cache.misses());
        rejected.increment(cache.rejected());
    }

    /** Stops adding the counts of the caches to the registries, as at the shutdown of the application context. */
    @Override
    public void close() {
        listenerRemovals.forEach(Runnable::run);
        listenerRemovals.clear();
    }
}
