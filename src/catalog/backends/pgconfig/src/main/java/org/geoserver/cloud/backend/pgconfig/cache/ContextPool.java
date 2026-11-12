/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A bounded pool of zstd contexts, each used by one thread at a time.
 *
 * <p>A thread finding the pool empty creates a new context. A context released to a pool holding {@code maxIdle} idle
 * contexts is closed instead, because zstd frees the native memory of a context only on close.
 *
 * @param <C> the type of the contexts
 */
final class ContextPool<C> implements AutoCloseable {

    private final ConcurrentLinkedQueue<C> idle = new ConcurrentLinkedQueue<>();
    private final AtomicInteger idleCount = new AtomicInteger();
    private final int maxIdle;
    private final Supplier<C> factory;
    private final Consumer<C> closer;

    ContextPool(int maxIdle, Supplier<C> factory, Consumer<C> closer) {
        this.maxIdle = maxIdle;
        this.factory = factory;
        this.closer = closer;
    }

    /** Takes an idle context, or creates one. The caller must {@link #release(Object) release} it after use. */
    C acquire() {
        C context = idle.poll();
        if (context == null) {
            return factory.get();
        }
        idleCount.decrementAndGet();
        return context;
    }

    void release(C context) {
        if (reserveIdleSlot()) {
            idle.offer(context);
        } else {
            closer.accept(context);
        }
    }

    // Counting before the offer keeps the queue at maxIdle contexts or fewer, even with concurrent releases
    private boolean reserveIdleSlot() {
        if (idleCount.incrementAndGet() <= maxIdle) {
            return true;
        }
        idleCount.decrementAndGet();
        return false;
    }

    /**
     * Closes the idle contexts, at shutdown. The pool stays usable: a context in use at that time, or created later,
     * goes back to the pool on release.
     */
    @Override
    public void close() {
        C context = idle.poll();
        while (context != null) {
            idleCount.decrementAndGet();
            closer.accept(context);
            context = idle.poll();
        }
    }
}
