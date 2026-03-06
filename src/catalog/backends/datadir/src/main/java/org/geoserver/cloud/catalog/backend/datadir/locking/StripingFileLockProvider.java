/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.catalog.backend.datadir.locking;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.Resource;
import org.geoserver.util.IOUtils;
import org.geotools.util.logging.Logging;

/**
 * A lock provider based exclusively on {@link FileLock file system locks}, hence useful for inter-process locking but
 * unsuitable for locking across threads on the same JVM instance.
 *
 * <p><b>Striping strategy:</b> Each lock key is SHA-1 hashed (in {@link StripingFileLock}) to one of 1,048,576 (1M)
 * buckets. A lock is a {@link FileLock} on a 1-byte region at the bucket's offset within the shared lock file. This
 * avoids creating a file per lock key and ensures all processes contend on the same file descriptor.
 *
 * <p><b>ThreadLocal lock caching:</b> {@link StripingFileLock} instances are cached per-thread in a {@link ThreadLocal}
 * map, allowing reentrancy — if the same thread acquires the same key again, the existing lock's counter is incremented
 * rather than attempting a new file lock.
 *
 * <p><b>Collision detection:</b> The {@link #bucketsHeldForKey} map tracks which lock key currently holds each bucket,
 * and {@link #threadIdHoldingKey} tracks the reentrancy count per key. If two different keys hash to the same bucket
 * and one is already held, an {@link IllegalStateException} is thrown to signal the collision.
 *
 * @see DistributedFileLockProvider
 * @see ChainedLockProvider
 * @see StripingFileLock
 */
class StripingFileLockProvider implements LockProvider, Closeable {

    static final Logger LOGGER = Logging.getLogger(StripingFileLockProvider.class);

    /** The wait to occur in case the lock cannot be acquired */
    private int waitBeforeRetry = 50;
    /** max lock attempts */
    private int maxLockAttempts = 120 * 1000 / waitBeforeRetry;

    /** Holds the lock key by bucket index to recognize hash collisions */
    Map<Long, String> bucketsHeldForKey = new ConcurrentHashMap<>();
    /** Holds lock counter by lock key to aid in reentrancy */
    Map<String, AtomicInteger> threadIdHoldingKey = new ConcurrentHashMap<>();

    private File locksFile;

    private FileChannel channel;

    @SuppressWarnings("java:S5164")
    private static final ThreadLocal<Map<String, StripingFileLock>> LOCKS =
            ThreadLocal.withInitial(ConcurrentHashMap::new);

    public StripingFileLockProvider(File locksFile) {
        if (!locksFile.isFile()) {
            throw new IllegalArgumentException(locksFile.getAbsolutePath() + " is not a file");
        }
        if (!locksFile.canWrite()) {
            throw new IllegalArgumentException(locksFile.getAbsolutePath() + " is not writable");
        }
        this.locksFile = locksFile;
    }

    @Override
    public Resource.Lock acquire(final String lockKey) {
        StripingFileLock lock = getLock(lockKey);
        return lock.lock();
    }

    private StripingFileLock getLock(String lockKey) {
        return LOCKS.get().computeIfAbsent(lockKey, key -> new StripingFileLock(key, this));
    }

    @Override
    public void close() {
        FileChannel ch = this.channel;
        this.channel = null;
        IOUtils.closeQuietly(ch);
    }

    FileChannel getChannel() {
        FileChannel ch = this.channel;
        if (ch == null || !ch.isOpen()) {
            final File file = getFile();
            synchronized (this) {
                if (this.channel == null) {
                    ch = openChannel(file);
                    this.channel = ch;
                } else {
                    ch = this.channel;
                }
            }
        }
        return ch;
    }

    @SuppressWarnings("resource")
    private FileChannel openChannel(File file) {
        try {
            return new RandomAccessFile(file, "rw").getChannel();
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Locks file does not exist: " + file, e);
        }
    }

    public void setWaitBeforeRetry(int millis) {
        if (millis <= 0) {
            throw new IllegalArgumentException("waitBeforeRetry must be positive or zero");
        }
        this.waitBeforeRetry = millis;
    }

    int getWaitBeforeRetry() {
        return this.waitBeforeRetry;
    }

    public void setMaxLockAttempts(int maxAttempts) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxLockAttempts must be positive or zero");
        }
        this.maxLockAttempts = maxAttempts;
    }

    int getMaxLockAttempts() {
        return this.maxLockAttempts;
    }

    File getFile() {
        final File file = this.locksFile;
        Objects.requireNonNull(file, "Locks file not provided");
        if (!file.exists()) {
            File parent = file.getParentFile();
            parent.mkdirs();
            if (!parent.isDirectory()) {
                throw new IllegalStateException("Locks directory does not exist or is not a directory: " + parent);
            }
            try {
                boolean created = file.createNewFile();
                if (created) {
                    LOGGER.finest("Created locks file");
                } else {
                    LOGGER.info("Locks file created by another process");
                }
            } catch (IOException e) {
                throw new IllegalStateException("Error creating locks file " + file, e);
            }
        }
        if (!file.isFile()) {
            throw new IllegalStateException("Locks file is not a file or cannot be created: " + file);
        }
        return file;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + locksFile;
    }
}
