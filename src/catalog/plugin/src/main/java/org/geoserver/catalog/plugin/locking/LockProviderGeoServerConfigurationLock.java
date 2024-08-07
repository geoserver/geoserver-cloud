/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.locking;

import static org.geoserver.GeoServerConfigurationLock.LockType.WRITE;

import lombok.NonNull;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.Resource.Lock;

/**
 * Extends {@link GeoServerConfigurationLock} with cluster-wide locking semantics provided by {@link
 * LockProvider}.
 *
 * @since 1.0
 */
public class LockProviderGeoServerConfigurationLock extends GeoServerConfigurationLock {

    private static final String LOCK_NAME = "global_datadir_lock";

    private @NonNull LockProvider lockProvider;

    private static class ReentrantGlobalLock {

        private int writeHoldCount;
        private Lock globalLock;

        public void lock(LockProvider lockProvider) {
            if (0 == writeHoldCount) {
                globalLock = lockProvider.acquire(LOCK_NAME);
            }
            writeHoldCount++;
        }

        public boolean unlock() {
            if (null != globalLock) {
                --writeHoldCount;
                if (writeHoldCount == 0) {
                    globalLock.release();
                    globalLock = null;
                    return true;
                }
            }
            return false;
        }

        public boolean isWriteLocked() {
            return globalLock != null;
        }

        public void forceUnlock() {
            writeHoldCount = 1;
            unlock();
            writeHoldCount = 0;
        }
    }

    private static final ThreadLocal<ReentrantGlobalLock> GLOBAL =
            ThreadLocal.withInitial(ReentrantGlobalLock::new);

    public LockProviderGeoServerConfigurationLock(@NonNull LockProvider lockProvider) {
        super();
        this.lockProvider = lockProvider;
    }

    @Override
    public boolean isWriteLocked() {
        if (isEnabled()) {
            final boolean jvmWriteLocked = super.isWriteLocked();
            final boolean globalWriteLocked = GLOBAL.get().isWriteLocked();
            if (jvmWriteLocked != globalWriteLocked) {
                String msg =
                        """
                        local JVM and global write lock status discrepancy: \
                        globally held lock count: %d, jvm locked: %s. The global lock will forcedly be released.
                        """
                                .formatted(GLOBAL.get().writeHoldCount, jvmWriteLocked);

                GLOBAL.get().forceUnlock();
                GLOBAL.remove();
                throw new IllegalStateException(msg);
            }
            return jvmWriteLocked || globalWriteLocked;
        }
        return false;
    }

    private void unlockGloblal() {
        if (GLOBAL.get().unlock()) {
            GLOBAL.remove();
        }
    }

    private void lockGloblal() {
        try {
            GLOBAL.get().lock(lockProvider);
        } catch (RuntimeException e) {
            super.unlock();
            throw e;
        }
    }

    @Override
    public void lock(LockType type) {
        if (isEnabled()) {
            // JVM lock
            super.lock(type);
            // cluster lock
            if (WRITE == type) {
                lockGloblal();
            }
        }
    }

    @Override
    public boolean tryLock(LockType type) {
        if (isEnabled()) {
            final boolean jvmLock = super.tryLock(type);
            if (jvmLock && WRITE == type) {
                lockGloblal();
            }
            return jvmLock;
        }
        return true;
    }

    @Override
    public void tryUpgradeLock() {
        if (isEnabled()) {
            super.tryUpgradeLock();
        }
    }

    @Override
    public void unlock() {
        if (isEnabled()) {
            try {
                unlockGloblal();
            } finally {
                super.unlock();
            }
        }
    }
}
