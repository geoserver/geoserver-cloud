/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.catalog.backend.datadir.locking;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import org.geoserver.platform.resource.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link StripingFileLock} behavior including bucket determinism, distribution, and
 * reentrancy. Tests go through {@link StripingFileLockProvider#acquire(String)} which manages the
 * ThreadLocal cache and {@code bucketsHeldForKey} map correctly.
 */
class StripingFileLockTest {

    @TempDir
    File tempDir;

    private StripingFileLockProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        File file = new File(tempDir, "test.locks");
        file.createNewFile();
        provider = new StripingFileLockProvider(file);
    }

    @AfterEach
    void tearDown() {
        provider.close();
    }

    @Test
    void sameKeyIsDeterministic() {
        // Acquiring the same key twice (reentrant) succeeds because the ThreadLocal cache
        // returns the same StripingFileLock instance, which maps to the same bucket
        Resource.Lock lock1 = provider.acquire("myKey");
        Resource.Lock lock2 = provider.acquire("myKey");
        // Both calls succeed — the second is reentrant on the same lock object
        assertThat(lock2).isSameAs(lock1);
        lock2.release();
        lock1.release();
    }

    @Test
    void differentKeysMapToDifferentBuckets() {
        // With SHA1 hashing and 1M buckets, different keys should map to different buckets.
        // If they collided, the second acquire would throw IllegalStateException.
        Resource.Lock lock1 = provider.acquire("alpha");
        Resource.Lock lock2 = provider.acquire("beta");
        assertThat(lock2).isNotSameAs(lock1);
        lock2.release();
        lock1.release();
    }

    @Test
    void reentrancyRequiresMultipleReleases() {
        // Acquire the same key three times — all return the same lock instance
        Resource.Lock lock1 = provider.acquire("reentrantKey");
        Resource.Lock lock2 = provider.acquire("reentrantKey");
        Resource.Lock lock3 = provider.acquire("reentrantKey");
        assertThat(lock2).isSameAs(lock1);
        assertThat(lock3).isSameAs(lock1);

        // First two releases decrement the counter but don't release the file lock
        lock3.release();
        lock2.release();

        // The key should still be lockable (same thread reentrant) before final release
        Resource.Lock lock4 = provider.acquire("reentrantKey");
        assertThat(lock4).isSameAs(lock1);
        lock4.release();

        // Final release actually frees the file lock
        lock1.release();
    }

    @Test
    void manyDistinctKeysAllAcquireSuccessfully() {
        // Verifies that the SHA1 bucket distribution works for a reasonable number of keys
        int numKeys = 50;
        Resource.Lock[] locks = new Resource.Lock[numKeys];
        for (int i = 0; i < numKeys; i++) {
            locks[i] = provider.acquire("distinctKey-" + i);
            assertThat(locks[i]).isNotNull();
        }
        for (int i = numKeys - 1; i >= 0; i--) {
            locks[i].release();
        }
    }
}
