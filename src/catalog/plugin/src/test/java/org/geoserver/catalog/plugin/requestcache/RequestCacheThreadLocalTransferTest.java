/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.requestcache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RequestCacheThreadLocalTransferTest {

    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private final RequestCacheThreadLocalTransfer transfer = new RequestCacheThreadLocalTransfer();

    private final Map<String, Object> storage = new HashMap<>();

    @AfterEach
    void closeScope() {
        RequestCacheScope.close();
        worker.shutdownNow();
    }

    @Test
    void transferAppliesToWorker() throws Exception {
        RequestCacheScope.open(10);
        RequestCache cache = RequestCacheScope.current();
        transfer.collect(storage);

        RequestCache applied = onWorker(this::applyThenCurrent);
        RequestCache afterCleanup = onWorker(this::cleanupThenCurrent);

        assertThat(applied).isSameAs(cache);
        assertThat(afterCleanup).isNull();
        assertThat(cache.isDead()).isFalse();
        assertThat(RequestCacheScope.current()).isSameAs(cache);
    }

    // a pooled worker may still hold a cache: the task of a thread without a scope must not read it
    @Test
    void noScopeUnbindsWorker() throws Exception {
        transfer.collect(storage);
        onWorker(RequestCacheThreadLocalTransferTest::bindLeftoverCache);

        RequestCache applied = onWorker(this::applyThenCurrent);

        assertThat(storage).isEmpty();
        assertThat(applied).isNull();
    }

    // a worker running after the end of its request holds the dead cache, and gives nothing to its own tasks
    @Test
    void deadCacheNotCollected() {
        RequestCache dead = new RequestCache(10);
        dead.kill();
        RequestCacheScope.apply(dead);

        transfer.collect(storage);

        assertThat(storage).isEmpty();
    }

    private <T> T onWorker(Callable<T> task) throws Exception {
        return worker.submit(task).get();
    }

    private @Nullable RequestCache applyThenCurrent() {
        transfer.apply(storage);
        return RequestCacheScope.current();
    }

    private @Nullable RequestCache cleanupThenCurrent() {
        transfer.cleanup();
        return RequestCacheScope.current();
    }

    private static @Nullable Void bindLeftoverCache() {
        RequestCacheScope.apply(new RequestCache(10));
        return null;
    }
}
