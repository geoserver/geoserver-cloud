/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.resource;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.NonNull;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheMetrics;

/**
 * Registers the statistics of a {@link ResourceStoreCache} with Micrometer: the meters of its content, as for the
 * entity caches, and a {@code resourcestore.index.rows} gauge.
 *
 * <p>This binder keeps Micrometer types out of {@link ResourceStoreCache}, see {@link EntityCacheMetrics}.
 */
public class ResourceStoreCacheMetrics implements MeterBinder {

    private final ResourceStoreCache cache;

    public ResourceStoreCacheMetrics(@NonNull ResourceStoreCache cache) {
        this.cache = cache;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        EntityCacheMetrics contentMetrics = new EntityCacheMetrics(cache.name(), cache.contentCache());
        contentMetrics.bindTo(registry);
        Gauge.builder("resourcestore.index.rows", cache, ResourceStoreCache::size)
                .description("Rows of the resource store held by the index; 0 until the index is loaded")
                .tags(EntityCacheMetrics.gaugeTags(cache.name()))
                .register(registry);
    }
}
