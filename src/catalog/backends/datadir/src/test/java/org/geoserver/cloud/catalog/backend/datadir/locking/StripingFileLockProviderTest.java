/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.catalog.backend.datadir.locking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import org.geoserver.platform.resource.Resource.Lock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StripingFileLockProviderTest {

    @TempDir
    File tempDir;

    private File createLocksFile() throws IOException {
        File file = new File(tempDir, "test.locks");
        file.createNewFile();
        return file;
    }

    @Test
    void acquireAndRelease() throws Exception {
        StripingFileLockProvider provider = new StripingFileLockProvider(createLocksFile());
        Lock lock = provider.acquire("myKey");
        assertThat(lock).isNotNull();
        lock.release();
        provider.close();
    }

    @Test
    void reentrantLockingIncrements() throws Exception {
        StripingFileLockProvider provider = new StripingFileLockProvider(createLocksFile());
        Lock lock1 = provider.acquire("myKey");
        Lock lock2 = provider.acquire("myKey");
        // both return the same StripingFileLock due to ThreadLocal caching
        assertThat(lock2).isSameAs(lock1);
        // release twice
        lock2.release();
        lock1.release();
        provider.close();
    }

    @Test
    void differentKeysProduceDifferentLocks() throws Exception {
        StripingFileLockProvider provider = new StripingFileLockProvider(createLocksFile());
        Lock lock1 = provider.acquire("key1");
        Lock lock2 = provider.acquire("key2");
        assertThat(lock2).isNotSameAs(lock1);
        lock1.release();
        lock2.release();
        provider.close();
    }

    @Test
    void constructorRejectsNonExistentFile() {
        File missing = new File(tempDir, "nonexistent.locks");
        assertThatThrownBy(() -> new StripingFileLockProvider(missing)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorRejectsNonWritableFile() throws Exception {
        File file = createLocksFile();
        file.setWritable(false);
        try {
            assertThatThrownBy(() -> new StripingFileLockProvider(file)).isInstanceOf(IllegalArgumentException.class);
        } finally {
            file.setWritable(true);
        }
    }

    @Test
    void closeClosesChannel() throws Exception {
        StripingFileLockProvider provider = new StripingFileLockProvider(createLocksFile());
        // force channel open
        provider.acquire("key").release();
        provider.close();
        // after close, acquiring should reopen channel (lazy init)
        Lock lock = provider.acquire("key2");
        assertThat(lock).isNotNull();
        lock.release();
        provider.close();
    }

    @Test
    void setWaitBeforeRetryRejectsNonPositive() throws Exception {
        StripingFileLockProvider provider = new StripingFileLockProvider(createLocksFile());
        assertThatThrownBy(() -> provider.setWaitBeforeRetry(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.setWaitBeforeRetry(-1)).isInstanceOf(IllegalArgumentException.class);
        provider.close();
    }

    @Test
    void setMaxLockAttemptsRejectsNonPositive() throws Exception {
        StripingFileLockProvider provider = new StripingFileLockProvider(createLocksFile());
        assertThatThrownBy(() -> provider.setMaxLockAttempts(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.setMaxLockAttempts(-1)).isInstanceOf(IllegalArgumentException.class);
        provider.close();
    }
}
