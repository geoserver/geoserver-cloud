/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bounds what a bulk reader puts in an {@link EntityCache}.
 *
 * <p>A GWC listing of a catalog larger than the cache would push out the objects used most if the cache put each row
 * read. A GWC listing reads its objects by id in batches, through reads that honor the budget. A reader binds a budget
 * to its thread with {@link #bind()}. The cache stops putting rows once the weight charged reaches the limit. The
 * write-through of catalog listings ignores it.
 *
 * <pre>{@code
 * try (PutBudget.Binding binding = PutBudget.halfOf(cache).bind()) {
 *     readInBatches();
 * }
 * }</pre>
 */
public final class PutBudget {

    private static final ThreadLocal<PutBudget> BOUND = new ThreadLocal<>();

    private final long limitBytes;
    private final AtomicLong charged = new AtomicLong();

    /** Creates a budget spent once the weight charged reaches the limit. */
    public PutBudget(long limitBytes) {
        this.limitBytes = limitBytes;
    }

    /** Returns a budget of half the maximum weight of the cache. The other half stays for the objects already in it. */
    public static PutBudget halfOf(EntityCache cache) {
        return new PutBudget(cache.maximumWeight() / 2);
    }

    /** Returns a budget that is never spent. */
    public static PutBudget unlimited() {
        return new PutBudget(Long.MAX_VALUE);
    }

    /** Whether the weight charged reached the limit. */
    public boolean isSpent() {
        return charged.get() >= limitBytes;
    }

    /** Adds the weight of an entry stored in the cache. */
    public void charge(long weight) {
        charged.addAndGet(weight);
    }

    /** The weight charged so far, in bytes. */
    public long charged() {
        return charged.get();
    }

    /**
     * Binds this budget to the calling thread, replacing the one bound before. Closing the binding restores the budget
     * found when it was opened.
     */
    public Binding bind() {
        return new Binding(BOUND.get(), this);
    }

    /** Returns the budget bound to the calling thread, empty if there is none. */
    public static Optional<PutBudget> current() {
        return Optional.ofNullable(BOUND.get());
    }

    /** The binding of a budget to a thread, for a try-with-resources block. */
    public static final class Binding implements AutoCloseable {

        private final PutBudget previous;
        private boolean closed;

        private Binding(PutBudget previous, PutBudget bound) {
            this.previous = previous;
            BOUND.set(bound);
        }

        /** Restores the budget bound when this binding was opened. Closing a second time does nothing. */
        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (previous == null) {
                BOUND.remove();
            } else {
                BOUND.set(previous);
            }
        }
    }
}
