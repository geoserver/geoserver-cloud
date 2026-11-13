/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ContextPoolTest {

    private final Queue<FakeContext> created = new ConcurrentLinkedQueue<>();

    @Test
    void closesContextReturnedToFullPool() {
        ContextPool<FakeContext> pool = newPool(1);
        FakeContext first = pool.acquire();
        FakeContext second = pool.acquire();

        pool.release(first);
        pool.release(second);

        assertThat(first.isClosed()).isFalse();
        assertThat(second.isClosed()).isTrue();
        assertThat(pool.acquire()).isSameAs(first);
    }

    // without counting the reuse, each later release would close its context and the pool would create one per call
    @Test
    void reusedContextReturnsToPool() {
        ContextPool<FakeContext> pool = newPool(1);
        FakeContext context = pool.acquire();
        pool.release(context);

        FakeContext reused = pool.acquire();
        pool.release(reused);

        assertThat(reused).isSameAs(context);
        assertThat(reused.isClosed()).isFalse();
        assertThat(pool.acquire()).isSameAs(context);
    }

    @Test
    void boundsIdleContextsUnderContention() throws InterruptedException, ExecutionException {
        ContextPool<FakeContext> pool = newPool(1);

        runConcurrently(8, () -> acquireAndRelease(pool, 5_000));

        assertThat(created)
                .allSatisfy(context -> assertThat(context.closeCount()).isLessThanOrEqualTo(1));
        long open = created.stream().filter(context -> !context.isClosed()).count();
        assertThat(open).isLessThanOrEqualTo(1);
    }

    @Test
    void closeClosesIdleContexts() {
        ContextPool<FakeContext> pool = newPool(2);
        FakeContext first = pool.acquire();
        FakeContext second = pool.acquire();
        pool.release(first);
        pool.release(second);

        pool.close();

        assertThat(first.closeCount()).isEqualTo(1);
        assertThat(second.closeCount()).isEqualTo(1);
    }

    private ContextPool<FakeContext> newPool(int maxIdle) {
        return new ContextPool<>(maxIdle, this::newContext, FakeContext::close);
    }

    private FakeContext newContext() {
        FakeContext context = new FakeContext();
        created.add(context);
        return context;
    }

    private static void acquireAndRelease(ContextPool<FakeContext> pool, int cycles) {
        for (int i = 0; i < cycles; i++) {
            FakeContext context = pool.acquire();
            pool.release(context);
        }
    }

    private static void runConcurrently(int threads, Runnable task) throws InterruptedException, ExecutionException {
        List<Callable<Object>> tasks = Collections.nCopies(threads, Executors.callable(task));
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            for (Future<Object> done : executor.invokeAll(tasks)) {
                done.get();
            }
        }
    }

    private static final class FakeContext {
        private final AtomicInteger closeCount = new AtomicInteger();

        void close() {
            closeCount.incrementAndGet();
        }

        int closeCount() {
            return closeCount.get();
        }

        boolean isClosed() {
            return closeCount() > 0;
        }
    }
}
