/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.catalog;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheLifecycle;
import org.geoserver.cloud.backend.pgconfig.cache.PutBudget;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigLayerGroupRepository;
import org.geoserver.security.impl.GsCloudLayerGroupContainmentCache;
import org.geoserver.security.impl.LayerGroupWarmUp;

/**
 * Loads the layer groups of the pgconfig catalog and their members into the entity cache, ahead of their lookups. The
 * access checks look up the groups containing a layer by id. Without the warm-up, the first request after a start, a
 * reset or a reload runs one query per group.
 *
 * <p>{@link GsCloudLayerGroupContainmentCache} runs the warm-up at the start of each build.
 * {@link EntityCacheLifecycle} runs it in the background once the cache serves again after a reset or a reload: a reset
 * empties the cache without a build, and the build of a reload may run while the cache is suspended.
 *
 * <p>The warm-up puts half of the maximum weight of the cache at most. The other half stays for the objects read by
 * requests.
 *
 * <p>Only the services running the containment cache have the warm-up, when
 * {@code geoserver.security.layergroup-containmentcache} is true. The services without the containment cache, such as
 * WFS, do not load the groups: their access checks do not look up the groups containing a layer.
 */
@Slf4j(topic = "org.geoserver.cloud.backend.pgconfig.catalog")
public class PgconfigLayerGroupWarmUp implements LayerGroupWarmUp {

    private final PgconfigLayerGroupRepository groups;
    private final EntityCache cache;

    /** The cancellation of the last run started by {@link #warmUpInBackground()}. */
    private final AtomicReference<AtomicBoolean> lastRunCancelled = new AtomicReference<>(new AtomicBoolean());

    public PgconfigLayerGroupWarmUp(@NonNull PgconfigLayerGroupRepository groups, @NonNull EntityCache cache) {
        this.groups = groups;
        this.cache = cache;
    }

    /**
     * Puts the groups and their members in the cache, on the calling thread, until cancelled. Does nothing while the
     * cache is suspended: the cache would ignore the puts.
     */
    @Override
    public void warmUp(@NonNull BooleanSupplier cancelled) {
        if (cache.isSuspended()) {
            log.debug("Layer group warm-up skipped, the cache {} is suspended", cache.name());
            return;
        }
        PutBudget budget = PutBudget.halfOf(cache);
        long start = System.nanoTime();
        int groupsPut = groups.warmUp(cancelled, budget);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
        log.info(
                "Layer group warm-up put {} groups in the cache {}, {} bytes, in {} ms",
                groupsPut,
                cache.name(),
                budget.charged(),
                elapsed.toMillis());
    }

    /**
     * Runs {@link #warmUp(BooleanSupplier)} on the common pool of the JVM. A new call cancels the run in flight: that
     * run stops before its next page.
     */
    public void warmUpInBackground() {
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicBoolean previous = lastRunCancelled.getAndSet(cancelled);
        previous.set(true);
        CompletableFuture.runAsync(() -> runInBackground(cancelled));
    }

    // A failed warm-up costs speed only: the lookups read the groups instead
    private void runInBackground(AtomicBoolean cancelled) {
        try {
            warmUp(cancelled::get);
        } catch (RuntimeException e) {
            log.warn("Layer group warm-up failed, the lookups read the groups instead", e);
        }
    }
}
