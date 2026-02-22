/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.backend.pgconfig.resource;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.Resource.Lock;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;

/**
 * Adapts a spring-integration-jdbc's {@link LockRegistry} to a GeoServer {@link LockProvider}
 *
 * <p>Of most interest here is using a {@link LockRegistry} backed by a {@link
 * org.springframework.integration.jdbc.lock.DefaultLockRepository} to provide distributed locking
 * on a clustered environment using the database to hold the locks.
 *
 * @since 1.4
 */
@Slf4j
public class PgconfigLockProvider implements LockProvider {
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(LockAdapter.class);

    private JdbcLockRegistry registry;

    public PgconfigLockProvider(JdbcLockRegistry registry) {
        Objects.requireNonNull(registry);
        this.registry = registry;
    }

    @Override
    public Lock acquire(String path) {
        Objects.requireNonNull(path);
        log.debug("Acquiring lock on {}", path);

        java.util.concurrent.locks.Lock lock = registry.obtain(path);
        lock.lock();

        log.debug("Acquired lock on {}", path);
        return new LockAdapter(path, lock);
    }

    /** Adapts a {@link java.util.concurrent.locks.Lock} as a {@link org.geoserver.platform.resource.Resource.Lock} */
    private static class LockAdapter implements Lock {

        private String key;
        private java.util.concurrent.locks.Lock lock;
        private boolean released;

        public LockAdapter(String key, java.util.concurrent.locks.Lock lock) {
            Objects.requireNonNull(lock);
            this.key = key;
            this.lock = lock;
        }

        @Override
        public void release() {
            if (!released) {
                released = true;
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("Releasing lock on " + key);
                }
                this.lock.unlock();
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Released lock on " + key);
                }
            }
        }
    }
}
