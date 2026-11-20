/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.catalog.backend.datadir.locking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.digest.DigestUtils;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;

/**
 * Represents a single held lock backed by a byte-range {@link FileLock} on the shared lock file.
 *
 * <p>Each lock key is mapped to a bucket via SHA-1 hashing ({@link #getBucket(String)}), producing one of 1,048,576
 * (1M) possible buckets. The file lock is acquired on a 1-byte region at offset {@code bucket * 1} within the shared
 * lock file.
 *
 * <p><b>Reentrancy:</b> A {@code lockCount} field tracks how many times the current thread has acquired this lock.
 * Calling {@link #lock()} on an already-held lock increments the counter; calling {@link #release()} decrements it. The
 * underlying {@link FileLock} is only released when the count reaches zero.
 *
 * <p><b>Collision detection:</b> If two different lock keys hash to the same bucket and one is already held (by another
 * thread via the same JVM's channel), an {@link OverlappingFileLockException} is caught and an
 * {@link IllegalStateException} is thrown to signal the collision, rather than silently waiting.
 *
 * <p><b>Recovery:</b> Once half of the attempts have failed, each further failed attempt asks the provider to
 * {@link StripingFileLockProvider#recycleChannel() recycle the channel}, which it only does while this JVM holds no
 * file lock, so that a lock the file system attributes to this process without it knowing does not block the bucket
 * until the process exits. The channel is recycled at most once per call.
 *
 * @see StripingFileLockProvider
 */
class StripingFileLock implements Resource.Lock {
    static final Logger LOGGER = Logging.getLogger(StripingFileLock.class);

    private static final int LOCK_SIZE = Byte.BYTES;
    private static final int MAX_LOCKS = 1024 * 1024;

    private final StripingFileLockProvider provider;
    private final String lockKey;

    private FileLock fileLock;
    private int lockCount; // handle reentrancy
    private long bucket = -1L;

    StripingFileLock(final String lockKey, final StripingFileLockProvider provider) {
        this.lockKey = lockKey;
        this.provider = provider;
    }

    /** @throws IllegalStateException if failed to acquire a lock after max attempts */
    public StripingFileLock lock() {
        if (lockCount > 0) {
            ++lockCount;
        } else {
            this.bucket = getBucket(lockKey);
            finest("Mapped lock key %s to bucket %,d on locks file %s", lockKey, bucket, provider.getFile());
            this.fileLock = acquireWithRetries();
            this.lockCount = 1;
        }
        finer("Acquired lock on %s, bucket %d, lock counter: %d", lockKey, this.bucket, this.lockCount);
        return this;
    }

    /**
     * Tries to lock the bucket until it succeeds or the attempts run out. Past half the attempts, the channel is closed
     * once to drop any lock it holds without this JVM knowing, see {@link StripingFileLockProvider#recycleChannel()}.
     *
     * @throws IllegalStateException if failed to acquire a lock after max attempts
     */
    private FileLock acquireWithRetries() {
        final int maxLockAttempts = provider.getMaxLockAttempts();
        final int recycleAfter = maxLockAttempts / 2;
        int recycledAt = 0;
        boolean reopened = false;
        for (int attempt = 1; attempt <= maxLockAttempts; attempt++) {
            Optional<FileLock> lock;
            try {
                lock = acquire(bucket, lockKey);
            } catch (ChannelError e) {
                reopenChannel(reopened, e);
                reopened = true;
                continue;
            }
            if (lock.isPresent()) {
                if (recycledAt > 0) {
                    info(
                            "Acquired lock on %s (bucket %,d) after closing the locks file at attempt %,d",
                            lockKey, bucket, recycledAt);
                }
                return lock.get();
            }
            if (recycledAt == 0 && attempt >= recycleAfter && provider.recycleChannel()) {
                recycledAt = attempt;
                warning(
                        "Unable to lock on %s (bucket %,d) after %,d attempts while this process holds no lock on "
                                + "%s, closed it to release any lock the file system still attributes to it",
                        lockKey, bucket, recycledAt, provider.getFile());
            }
            final int waitBeforeRetry = provider.getWaitBeforeRetry();
            finest(
                    "Unable to lock on %s (bucket %,d), retrying in %dms (attempt %d/%d)...",
                    lockKey, bucket, waitBeforeRetry, attempt, maxLockAttempts);
            sleep(waitBeforeRetry);
        }
        throw new IllegalStateException(
                String.format("Failed to get a lock on key %s after %d attempts", lockKey, maxLockAttempts));
    }

    /**
     * Reopens the channel after an I/O error of a lock attempt, once. The handle under the channel can die: on an SMB
     * share mounted without persistent handles, a reconnect drops the open and every later lock call on the channel
     * fails, typically with "Bad file descriptor", until it is reopened. Fails if the channel was reopened already, or
     * if a lock is held on the channel, which is reopened when that lock is released.
     *
     * @throws IllegalStateException if the channel cannot be reopened
     */
    private void reopenChannel(boolean reopened, ChannelError e) {
        if (reopened || !provider.recycleChannel()) {
            throw new IllegalStateException("Unexpected error acquiring lock", e.getCause());
        }
        warning(
                "Error locking %s (bucket %,d) on %s, closed it to reopen it: %s",
                lockKey, bucket, provider.getFile(), e.getCause().getMessage());
    }

    @Override
    public void release() {
        finer("Releasing lock on %s, lock counter: %d", lockKey, lockCount);
        --lockCount;
        if (lockCount == 0) {
            FileLock lock = this.fileLock;
            this.fileLock = null;
            final String key = this.lockKey;
            final long assignedBucket = this.bucket;
            IOException releaseError = null;
            try {
                // not valid if the channel was closed meanwhile, which released it
                if (lock != null && lock.isValid()) {
                    lock.release();
                    finest("Released lock on %s, lock counter: %d", key, lockCount);
                }
            } catch (IOException e) {
                releaseError = e;
            } finally {
                // unregister even if releasing failed, so recycling the channel can still drop the lock, and only for
                // this key, as another key hashing to the same bucket may have taken the bucket since
                provider.bucketsHeldForKey.remove(assignedBucket, key);
                provider.threadIdHoldingKey.remove(key);
            }
            if (releaseError != null) {
                finer("Error releasing lock on %s: %s", key, releaseError.getMessage());
                // a handle that died under the lock fails its release too: reopen the channel now rather than at the
                // next lock, which also drops the lock if the file system still attributes it to this handle
                if (provider.recycleChannel()) {
                    warning(
                            "Error releasing lock on %s, closed the locks file to reopen it: %s",
                            key, releaseError.getMessage());
                }
                throw new IllegalStateException("Failure while trying to release lock for key " + key, releaseError);
            }
        } else {
            finer("Released lock reentrancy on %s, lock counter: %d", lockKey, lockCount);
        }
    }

    private Optional<FileLock> acquire(final long bucket, final String lockKey) {
        final long size = LOCK_SIZE;
        final long position = size * bucket;
        final boolean shared = false;
        FileLock lock = null;
        // keeps provider.recycleChannel() from closing the channel while we use it
        final Lock channelInUse = provider.channelReadLock();
        channelInUse.lock();
        try {
            FileChannel channel = provider.getChannel();
            // if tryLock returns null, the lock is held by another process
            lock = channel.tryLock(position, size, shared);
            if (lock != null) {
                // register before the channel can be recycled
                provider.bucketsHeldForKey.put(bucket, lockKey);
            }
        } catch (OverlappingFileLockException heldByAnotherThreadOnThisJVM) {
            finest(heldByAnotherThreadOnThisJVM.getMessage());
            String lockedKey = provider.bucketsHeldForKey.get(bucket);
            if (lockedKey != null && !lockKey.equals(lockedKey)) {
                String msg = String.format(
                        "Lock collision: lock for bucket %d is held for key %s. Can't lock key %s.",
                        bucket, lockedKey, lockKey);
                severe(msg);
                throw new IllegalStateException(msg);
            } else {
                // held by another thread, which may not have registered it yet, or by no thread at all after a failed
                // release, in which case only recycling the channel drops it
                finer("FileLock is held by another thread");
            }
        } catch (ClosedChannelException e) {
            throw new IllegalStateException("Locks file channel was closed unexpectedly", e);
        } catch (IOException e) {
            // lock() decides whether to reopen the channel, once this attempt has let go of it
            throw new ChannelError(e);
        } finally {
            channelInUse.unlock();
        }
        return Optional.ofNullable(lock);
    }

    /** An I/O error from the locks file channel, see {@link #lock()} */
    private static final class ChannelError extends RuntimeException {
        private static final long serialVersionUID = 1L;

        ChannelError(IOException cause) {
            super(cause);
        }
    }

    /** Whether this lock is held and its file lock is still valid, for tests */
    boolean isFileLockValid() {
        FileLock lock = this.fileLock;
        return lock != null && lock.isValid();
    }

    static long getBucket(String lockKey) {
        // Simply hashing the lock key generated a significant number of collisions,
        // doing the SHA1 digest of it provides a much better distribution
        @SuppressWarnings("java:S4790") // sha1 is good enough for hashing
        byte[] sha1 = DigestUtils.sha1(lockKey);
        long hash = ByteBuffer.wrap(sha1).getLong();
        return Math.abs(hash) % MAX_LOCKS;
    }

    private static void severe(String msg, Object... msgArgs) {
        log(Level.SEVERE, msg, msgArgs);
    }

    private static void warning(String msg, Object... msgArgs) {
        log(Level.WARNING, msg, msgArgs);
    }

    private static void info(String msg, Object... msgArgs) {
        log(Level.INFO, msg, msgArgs);
    }

    private static void finer(String msg, Object... msgArgs) {
        log(Level.FINER, msg, msgArgs);
    }

    private static void finest(String msg, Object... msgArgs) {
        log(Level.FINEST, msg, msgArgs);
    }

    private static void log(Level level, String msg, Object... msgArgs) {
        if (LOGGER.isLoggable(level)) {
            String message = String.format(msg, msgArgs);
            if ("true".equals(System.getProperty("locks.log-stack-trace"))) {
                Throwable trace = new Exception("debugging aid stack trace").fillInStackTrace();
                LOGGER.log(level, message, trace);
            } else {
                LOGGER.log(level, message);
            }
        }
    }

    private void sleep(long waitBeforeRetry) {
        try {
            Thread.sleep(waitBeforeRetry);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
