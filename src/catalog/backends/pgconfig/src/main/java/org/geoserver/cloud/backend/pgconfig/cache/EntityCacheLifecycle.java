/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerReinitializer;
import org.geoserver.config.impl.GeoServerLifecycleHandler;

/**
 * Suspends the entity caches while GeoServer reloads its configuration, and resumes them once GeoServer has loaded it.
 *
 * <p>Before a reload, GeoServer calls {@link #beforeReload()} and {@link #beforeReinitialize(GeoServer)}: the caches
 * are suspended and emptied, and the repositories read the database as without a cache. GeoServer calls
 * {@link #initialize(GeoServer)} once it has loaded its configuration, at startup and after a reload, and
 * {@link #onReload()} at the end of a reload, failed or not: the caches are emptied and serve lookups again. A reset of
 * GeoServer and its disposal empty the caches. A task can run once the caches serve again after a reset or a reload,
 * see {@link #setAfterServingAgain(Runnable)}.
 *
 * <p>A cache keeps no misses until the first {@link #initialize(GeoServer)}: a miss kept while the configuration loads
 * would make two instances starting on an empty database both create the default objects.
 */
@Slf4j(topic = "org.geoserver.cloud.backend.pgconfig.cache")
public class EntityCacheLifecycle implements GeoServerReinitializer, GeoServerLifecycleHandler {

    private final List<EntityCache> caches;

    private volatile Runnable afterServingAgain = () -> {};

    public EntityCacheLifecycle(@NonNull List<EntityCache> caches) {
        this.caches = List.copyOf(caches);
    }

    /**
     * Sets the task run once the caches serve again after a reset or a reload: at the end of {@link #onReset()} and of
     * {@link #onReload()}. The caches are then empty and not suspended: they keep what the task puts, such as the
     * objects loaded by a warm-up.
     *
     * <p>The task also runs at the end of the reset called by {@code GeoServerImpl.reload()} between
     * {@link #beforeReload()} and {@link #onReload()}. The caches are suspended then, and a warm-up returns at once.
     */
    public void setAfterServingAgain(@NonNull Runnable listener) {
        this.afterServingAgain = listener;
    }

    @Override
    public void beforeReinitialize(GeoServer geoServer) {
        suspend("beforeReinitialize");
    }

    @Override
    public void initialize(GeoServer geoServer) {
        resume("initialize");
    }

    @Override
    public void beforeReload() {
        suspend("beforeReload");
    }

    /** Ends the suspension of {@link #beforeReload()}, also for a reload that fails or does not reinitialize. */
    @Override
    public void onReload() {
        resume("onReload");
        afterServingAgain.run();
    }

    @Override
    public void onReset() {
        clear("onReset");
        afterServingAgain.run();
    }

    @Override
    public void onDispose() {
        clear("onDispose");
    }

    private void suspend(String phase) {
        for (EntityCache cache : caches) {
            cache.setSuspended(true);
            // a suspended cache keeps the entries stored before, and would serve them once resumed
            cache.clear();
        }
        log.info("{}: suspended and cleared the caches {}", phase, names());
    }

    private void resume(String phase) {
        for (EntityCache cache : caches) {
            // at startup the cache was not suspended, and the objects kept while the configuration loaded may be stale
            cache.clear();
            cache.setSuspended(false);
            cache.setInitialized(true);
        }
        log.info("{}: cleared and resumed the caches {}", phase, names());
    }

    private void clear(String phase) {
        for (EntityCache cache : caches) {
            cache.clear();
        }
        log.info("{}: cleared the caches {}", phase, names());
    }

    private List<String> names() {
        return caches.stream().map(EntityCache::name).toList();
    }
}
