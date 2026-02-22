/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.catalog.backend.datadir.locking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.geoserver.platform.resource.Resource.Lock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DistributedFileLockProviderTest {

    @TempDir
    File tempDir;

    @Test
    void acquireAndRelease() {
        DistributedFileLockProvider provider = new DistributedFileLockProvider(tempDir);
        Lock lock = provider.acquire("testKey");
        assertThat(lock).isNotNull();
        lock.release();
    }

    @Test
    void createsLockFileAndDirectory() {
        new DistributedFileLockProvider(tempDir);
        File locksDir = new File(tempDir, ".filelocks");
        assertThat(locksDir).isDirectory();
        File locksFile = new File(locksDir, "resourcestore.locks");
        assertThat(locksFile).isFile();
    }

    @Test
    void destroyCleansUpResources() throws Exception {
        DistributedFileLockProvider provider = new DistributedFileLockProvider(tempDir);
        provider.acquire("key").release();
        provider.destroy();
        // after destroy, the file lock provider is nulled out; a new instance can be created
        DistributedFileLockProvider provider2 = new DistributedFileLockProvider(tempDir);
        Lock lock = provider2.acquire("key");
        assertThat(lock).isNotNull();
        lock.release();
        provider2.destroy();
    }

    @Test
    void constructorWithNullBasePathFails() {
        assertThatThrownBy(() -> new DistributedFileLockProvider(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void multipleKeysSucceed() {
        DistributedFileLockProvider provider = new DistributedFileLockProvider(tempDir);
        Lock lock1 = provider.acquire("key1");
        Lock lock2 = provider.acquire("key2");
        Lock lock3 = provider.acquire("key3");
        assertThat(lock1).isNotNull();
        assertThat(lock2).isNotNull();
        assertThat(lock3).isNotNull();
        lock3.release();
        lock2.release();
        lock1.release();
    }

    @Test
    @SuppressWarnings("java:S2925") // Thread.sleep
    void sameKeyBlocksOnSecondThread() throws Exception {
        DistributedFileLockProvider provider = new DistributedFileLockProvider(tempDir);
        Lock lock = provider.acquire("blockedKey");

        AtomicBoolean secondThreadAcquired = new AtomicBoolean(false);
        CountDownLatch started = new CountDownLatch(1);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> future = executor.submit(() -> {
                started.countDown();
                // This should block because the memory lock is held by the main thread
                Lock secondLock = provider.acquire("blockedKey");
                secondThreadAcquired.set(true);
                secondLock.release();
            });

            started.await(1, TimeUnit.SECONDS);
            // Give the second thread time to attempt acquisition
            Thread.sleep(200);
            assertThat(secondThreadAcquired.get())
                    .as("Second thread should be blocked while first holds the lock")
                    .isFalse();

            // Release first lock, allowing second thread to proceed
            lock.release();
            future.get(5, TimeUnit.SECONDS);
            assertThat(secondThreadAcquired.get()).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }
}
