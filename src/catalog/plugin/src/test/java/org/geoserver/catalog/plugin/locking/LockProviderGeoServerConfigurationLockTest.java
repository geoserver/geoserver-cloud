/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.locking;

import static org.geoserver.GeoServerConfigurationLock.LockType.READ;
import static org.geoserver.GeoServerConfigurationLock.LockType.WRITE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.platform.resource.FileLockProvider;
import org.geoserver.platform.resource.LockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @since 1.0
 */
class LockProviderGeoServerConfigurationLockTest {

    @TempDir File mockDataDir;

    private LockProviderGeoServerConfigurationLock lock;

    @BeforeEach
    void beforeEach() {
        System.setProperty("CONFIGURATION_TRYLOCK_TIMEOUT", "100");
        LockProvider lockProvider = new FileLockProvider(mockDataDir);
        lock = new LockProviderGeoServerConfigurationLock(lockProvider);
    }

    @AfterEach
    void afterEach() {
        System.clearProperty("CONFIGURATION_TRYLOCK_TIMEOUT");
        assertFalse(lock.isWriteLocked(), "all locks shall have been released");
    }

    @Test
    @Timeout(1)
    void testLock_WriteLock() {
        assertNull(lock.getCurrentLock());
        lock.lock(WRITE);
        assertEquals(WRITE, lock.getCurrentLock());
        lock.unlock();
        assertNull(lock.getCurrentLock());
    }

    @Test
    @Timeout(1)
    void testLock_ReadLock() {
        assertNull(lock.getCurrentLock());
        lock.lock(READ);
        assertEquals(READ, lock.getCurrentLock());
        lock.unlock();
        assertNull(lock.getCurrentLock());
    }

    @Test
    @Timeout(1)
    void testLock_ReadLock_preserves_write_lock_if_alread_held() {
        assertNull(lock.getCurrentLock());
        lock.lock(WRITE);
        assertEquals(WRITE, lock.getCurrentLock());

        lock.lock(READ);
        assertEquals(
                WRITE,
                lock.getCurrentLock(),
                "A read lock request shall preserve the write lock if already held");
        lock.unlock();
    }

    @Test
    @Timeout(1)
    void testTryUpgradeLock_fais_if_no_previous_lock_is_held() {
        assertNull(lock.getCurrentLock());
        IllegalStateException ex = assertThrows(IllegalStateException.class, lock::tryUpgradeLock);
        assertThat(ex.getMessage(), containsString("No lock currently held"));
    }

    @Test
    @Timeout(1)
    void testTryUpgradeLock_fails_if_already_holds_a_write_lock() {
        assertNull(lock.getCurrentLock());
        lock.lock(WRITE);

        IllegalStateException ex = assertThrows(IllegalStateException.class, lock::tryUpgradeLock);
        assertThat(ex.getMessage(), containsString("Already owning a write lock"));
        // this case, contrary to when a read lock is held, but tryUpgradeLock() fails, does not
        // release the currently held lock
        assertEquals(WRITE, lock.getCurrentLock());
        lock.unlock();
    }

    @Test
    @Timeout(1)
    void testTryUpgradeLock() throws InterruptedException, ExecutionException {
        ExecutorService secondThread = Executors.newSingleThreadExecutor();
        try {
            lock.lock(READ);

            secondThread
                    .submit(
                            () -> {
                                assertTrue(lock.tryLock(READ));
                            })
                    .get();

            assertEquals(READ, lock.getCurrentLock());

            RuntimeException ex = assertThrows(RuntimeException.class, () -> lock.tryUpgradeLock());
            assertThat(
                    ex.getMessage(),
                    containsString("Failed to upgrade lock from read to write state"));

            assertNull(
                    lock.getCurrentLock(),
                    "lock should have been lost after a failed tryUpgradeLock()");

            lock.lock(READ);

            secondThread.submit(lock::unlock).get();

            lock.tryUpgradeLock();
            assertEquals(WRITE, lock.getCurrentLock());
        } finally {
            secondThread.shutdownNow();
            lock.unlock();
        }
    }

    @Test
    @Timeout(1)
    void testTryLock() {
        assertTrue(lock.tryLock(READ));
        assertEquals(READ, lock.getCurrentLock());
        lock.unlock();
        assertNull(lock.getCurrentLock());

        assertTrue(lock.tryLock(WRITE));
        assertEquals(WRITE, lock.getCurrentLock());
        lock.unlock();
        assertNull(lock.getCurrentLock());
    }

    @Test
    @Timeout(1)
    void testTryLock_false_if_write_lock_requested_while_holding_a_read_lock() {
        assertNull(lock.getCurrentLock());

        assertTrue(lock.tryLock(READ));
        assertEquals(READ, lock.getCurrentLock());

        assertFalse(lock.tryLock(WRITE));
        assertEquals(READ, lock.getCurrentLock());
        lock.unlock();
        assertNull(lock.getCurrentLock());
    }

    @Test
    @Timeout(1)
    void testTryLock_true_if_read_lock_requested_while_holding_a_write_lock() {
        assertTrue(lock.tryLock(WRITE));
        assertEquals(WRITE, lock.getCurrentLock());

        assertTrue(lock.tryLock(READ));
        assertEquals(
                WRITE,
                lock.getCurrentLock(),
                "tryLock(READ) while holding a write lock shall preserve the write lock");

        lock.unlock();
        assertNull(lock.getCurrentLock());
        assertFalse(lock.isWriteLocked());
    }

    @Test
    @Timeout(1)
    void testUnlock() {
        assertNull(lock.getCurrentLock());
        lock.unlock();
        lock.unlock();
        assertNull(lock.getCurrentLock());

        lock.lock(READ);
        lock.unlock();
        assertNull(lock.getCurrentLock());

        lock.lock(WRITE);
        lock.unlock();
        assertNull(lock.getCurrentLock());
    }

    @Test
    @Timeout(1)
    void testLock_ReadLockIsReentrant() {
        testLockIsReentrant(READ);
    }

    @Test
    @Timeout(1)
    void testLock_WriteLockIsReentrant() {
        testLockIsReentrant(WRITE);
    }

    private void testLockIsReentrant(LockType lockType) {
        assertNull(lock.getCurrentLock());
        // first time
        lock.lock(lockType);
        try {
            assertEquals(lockType, lock.getCurrentLock());
            try {
                // second acquire
                lock.lock(lockType);
                assertEquals(lockType, lock.getCurrentLock());
            } finally {
                // first release
                lock.unlock();
                assertEquals(
                        lockType,
                        lock.getCurrentLock(),
                        "%s lock should still be held".formatted(lockType));
            }
        } finally {
            // second release
            lock.unlock();
            assertNull(lock.getCurrentLock());
        }
        assertFalse(lock.isWriteLocked());
    }

    @Test
    @Timeout(1)
    void testTryReadLockIsReentrant() {
        testTryLockIsReentrant(READ);
    }

    @Test
    @Timeout(1)
    void testTryWriteLockIsReentrant() {
        testTryLockIsReentrant(WRITE);
    }

    private void testTryLockIsReentrant(final LockType lockType) {
        assertNull(lock.getCurrentLock());
        // common case scenario from nested calls:

        // first time
        try {
            assertTrue(lock.tryLock(lockType));
            assertEquals(lockType, lock.getCurrentLock());
            try {
                assertTrue(lock.tryLock(lockType));
                assertEquals(lockType, lock.getCurrentLock());
            } finally {
                // first release
                lock.unlock();
                assertEquals(
                        lockType,
                        lock.getCurrentLock(),
                        "%s lock should still be held".formatted(lockType));
            }
        } finally {
            // second release
            lock.unlock();
            assertNull(lock.getCurrentLock());
        }
    }
}
