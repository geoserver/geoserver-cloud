/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.catalog.backend.datadir.locking;

import static java.util.Objects.requireNonNull;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.Resource.Lock;
import org.geotools.util.logging.Logging;

/**
 * A {@link LockProvider} that sequentially acquires locks from two underlying providers.
 *
 * <p>It attempts to obtain locks in the order the providers were passed to the constructor.
 * If any acquisition fails, all previously held locks in the current chain are released
 * immediately to prevent deadlocks and resource leakage.</p>
 *
 * <p><b>Lock Ordering:</b> To ensure consistency, locks are released in the
 * reverse order of their acquisition (LIFO).</p>
 *
 * @see LockProvider
 */
public class ChainedLockProvider implements LockProvider {

    private static final Logger LOGGER = Logging.getLogger(ChainedLockProvider.class);

    private final LockProvider first;
    private final LockProvider second;

    /**
     * Creates a new provider that chains two separate locking mechanisms.
     *
     * @param first  the primary lock provider (acquired first)
     * @param second the secondary lock provider (acquired second)
     * @throws NullPointerException if either provider is null
     */
    public ChainedLockProvider(LockProvider first, LockProvider second) {
        this.first = requireNonNull(first, "First LockProvider must not be null");
        this.second = requireNonNull(second, "Second LockProvider must not be null");
    }

    /**
     * Acquires locks from both internal providers for the given path.
     *
     * <p>The acquisition is performed sequentially. If the second lock fails,
     * the first lock is automatically released before the exception is propagated.</p>
     *
     * @param path the resource identifier to lock
     * @return a composite {@link Lock} that manages both held locks
     * @throws RuntimeException if any provider fails to acquire its lock
     */
    @Override
    public Lock acquire(String path) {
        LOGGER.fine(() -> "Acquiring primary lock on [%s] using %s"
                .formatted(path, first.getClass().getSimpleName()));
        final Lock firstLock = first.acquire(path);

        try {
            LOGGER.fine(() -> "Acquiring secondary lock on [%s] using %s"
                    .formatted(path, second.getClass().getSimpleName()));
            final Lock secondLock = second.acquire(path);

            LOGGER.fine(() -> "Successfully acquired chained locks for [%s]".formatted(path));
            return new ChainedLock(firstLock, secondLock);

        } catch (RuntimeException e) {
            LOGGER.log(
                    Level.WARNING, e, () -> "Failed to acquire secondary lock on [%s]. Releasing primary lock from %s"
                            .formatted(path, first.getClass().getSimpleName()));

            firstLock.release();
            throw e;
        }
    }

    /**
     * A composite lock implementation that ensures symmetric release order.
     */
    private static class ChainedLock implements Lock {
        private final Lock first;
        private final Lock second;

        ChainedLock(Lock first, Lock second) {
            this.first = first;
            this.second = second;
        }

        /**
         * Releases the locks in reverse acquisition order (second, then first).
         * Uses a {@code finally} block to ensure the first lock is released even if
         * releasing the second lock throws an exception.
         */
        @Override
        public void release() {
            try {
                second.release();
            } finally {
                first.release();
            }
        }
    }
}
