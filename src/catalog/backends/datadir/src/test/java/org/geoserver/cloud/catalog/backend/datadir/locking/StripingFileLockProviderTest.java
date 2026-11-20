/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.catalog.backend.datadir.locking;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import org.geoserver.platform.resource.Resource.Lock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class StripingFileLockProviderTest {

    @TempDir
    File tempDir;

    private File locksFile;

    @BeforeEach
    void createateLocksFile(TestInfo testInfo) throws IOException {
        File file = new File(tempDir, testInfo.getDisplayName() + ".locks");
        file.createNewFile();
        this.locksFile = file;
    }

    @Test
    void acquireAndRelease() {
        StripingFileLockProvider provider = new StripingFileLockProvider(locksFile);
        Lock lock = provider.acquire("myKey");
        assertThat(lock).isNotNull();
        lock.release();
        provider.close();
    }

    @Test
    void reentrantLockingIncrements() {
        StripingFileLockProvider provider = new StripingFileLockProvider(locksFile);
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
    void differentKeysProduceDifferentLocks() {
        StripingFileLockProvider provider = new StripingFileLockProvider(locksFile);
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
    void constructorRejectsNonWritableFile() {
        locksFile.setWritable(false);
        assertThatThrownBy(() -> new StripingFileLockProvider(locksFile)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void closeClosesChannel() {
        StripingFileLockProvider provider = new StripingFileLockProvider(locksFile);
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
    void reopensChannelClosedElsewhere() throws IOException {
        try (StripingFileLockProvider provider = new StripingFileLockProvider(locksFile)) {
            FileChannel channel = provider.getChannel();
            // closed without going through provider.close(), which would also forget it
            channel.close();

            FileChannel reopened = provider.getChannel();
            assertThat(reopened).isNotSameAs(channel);
            assertThat(reopened.isOpen()).isTrue();

            Lock lock = provider.acquire("reopensChannelClosedElsewhere");
            lock.release();
        }
    }

    @Test
    void setWaitBeforeRetryRejectsNonPositive() {
        StripingFileLockProvider provider = new StripingFileLockProvider(locksFile);
        assertThatThrownBy(() -> provider.setWaitBeforeRetry(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.setWaitBeforeRetry(-1)).isInstanceOf(IllegalArgumentException.class);
        provider.close();
    }

    @Test
    void setMaxLockAttemptsRejectsNonPositive() {
        StripingFileLockProvider provider = new StripingFileLockProvider(locksFile);
        assertThatThrownBy(() -> provider.setMaxLockAttempts(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.setMaxLockAttempts(-1)).isInstanceOf(IllegalArgumentException.class);
        provider.close();
    }

    // The tests below use keys of their own: StripingFileLock instances are cached per thread and key in a static
    // ThreadLocal, so a key used by an earlier test on the same thread would bring that test's provider along

    @Test
    void recycleChannelOnlyWhileNoLockIsHeld() {
        try (StripingFileLockProvider provider = new StripingFileLockProvider(locksFile)) {
            Lock lock = provider.acquire("recycleChannelOnlyWhileNoLockIsHeld");
            FileChannel channel = provider.getChannel();
            assertThat(provider.recycleChannel()).isFalse();
            assertThat(channel.isOpen()).isTrue();

            lock.release();
            assertThat(provider.recycleChannel()).isTrue();
            assertThat(channel.isOpen()).isFalse();
            assertThat(provider.getChannel()).isNotSameAs(channel);
        }
    }

    /**
     * A lock the channel holds but no thread knows about, like one the SMB server granted while the client reported the
     * request as failed, is released by recycling the channel once half of the attempts have failed
     */
    @Test
    void recyclesChannelToReleaseALockNoThreadHolds() throws IOException {
        final String key = "recyclesChannelToReleaseALockNoThreadHolds";
        final long bucket = StripingFileLock.getBucket(key);
        StripingFileLockProvider provider = new StripingFileLockProvider(locksFile);
        provider.setWaitBeforeRetry(1);
        provider.setMaxLockAttempts(10);
        try {
            FileChannel channel = provider.getChannel();
            FileLock orphan = channel.tryLock(bucket, 1, false);
            assertThat(orphan).isNotNull();

            Lock lock = provider.acquire(key);

            assertThat(channel.isOpen()).isFalse();
            assertThat(orphan.isValid()).isFalse();
            assertThat(provider.bucketsHeldForKey).containsEntry(bucket, key);
            lock.release();
            assertThat(provider.bucketsHeldForKey).isEmpty();
        } finally {
            provider.close();
        }
    }

    @Test
    void doesNotRecycleChannelWhileALockIsHeld() throws IOException {
        final String key = "doesNotRecycleChannelWhileALockIsHeld";
        StripingFileLockProvider provider = new StripingFileLockProvider(locksFile);
        provider.setWaitBeforeRetry(1);
        provider.setMaxLockAttempts(10);
        try {
            StripingFileLock held = (StripingFileLock) provider.acquire(key + "-held");
            FileChannel channel = provider.getChannel();
            FileLock orphan = channel.tryLock(StripingFileLock.getBucket(key), 1, false);

            assertThatThrownBy(() -> provider.acquire(key))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Failed to get a lock on key %s after 10 attempts", key);

            assertThat(provider.getChannel()).isSameAs(channel);
            assertThat(held.isFileLockValid()).isTrue();
            assertThat(orphan.isValid()).isTrue();
            orphan.release();
            held.release();
        } finally {
            provider.close();
        }
    }

    @Test
    @Timeout(60)
    void recyclingNeverClosesTheChannelUnderAHeldLock() throws Exception {
        final StripingFileLockProvider provider = new StripingFileLockProvider(locksFile);
        final int threads = 8;
        final int iterations = 100;
        final AtomicBoolean done = new AtomicBoolean();
        final AtomicInteger recycled = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(threads + 1);
        try {
            Future<?> recycler = executor.submit(() -> {
                while (!done.get()) {
                    if (provider.recycleChannel()) {
                        recycled.incrementAndGet();
                    }
                    LockSupport.parkNanos(100_000);
                }
            });
            List<Future<?>> workers = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                final String key = "recyclingNeverClosesTheChannelUnderAHeldLock-" + t;
                workers.add(executor.submit(() -> {
                    for (int i = 0; i < iterations; i++) {
                        StripingFileLock lock = (StripingFileLock) provider.acquire(key);
                        try {
                            assertThat(lock.isFileLockValid()).isTrue();
                            Thread.yield();
                            assertThat(lock.isFileLockValid()).isTrue();
                        } finally {
                            lock.release();
                        }
                        LockSupport.parkNanos(1_000_000);
                    }
                    return null;
                }));
            }
            for (Future<?> worker : workers) {
                worker.get();
            }
            done.set(true);
            recycler.get();
        } finally {
            done.set(true);
            executor.shutdownNow();
            provider.close();
        }
        assertThat(recycled.get()).isPositive();
        assertThat(provider.bucketsHeldForKey).isEmpty();
    }

    /** Recycling the channel leaves alone a lock another process holds, and locking works once it lets go */
    @Test
    @Timeout(60)
    void lockHeldByAnotherProcess() throws Exception {
        final String key = "lockHeldByAnotherProcess";
        StripingFileLockProvider provider = new StripingFileLockProvider(locksFile);
        provider.setWaitBeforeRetry(10);
        provider.setMaxLockAttempts(20);
        Process holder = startLockHolder(locksFile, key);
        try {
            FileChannel channel = provider.getChannel();
            assertThatThrownBy(() -> provider.acquire(key))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Failed to get a lock on key %s after 20 attempts", key);
            // this JVM held no lock, so the channel was recycled, to no avail
            assertThat(channel.isOpen()).isFalse();

            // the holder releases its lock and exits once its input is closed
            holder.getOutputStream().close();
            assertThat(holder.waitFor(30, TimeUnit.SECONDS)).isTrue();

            Lock lock = provider.acquire(key);
            lock.release();
        } finally {
            holder.destroyForcibly();
            provider.close();
        }
    }

    private static Process startLockHolder(File file, String key) throws IOException {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("java.class.path");
        Process process = new ProcessBuilder(
                        java, "-cp", classpath, LockHolder.class.getName(), file.getAbsolutePath(), key)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
        // the holder writes one line once it holds the lock, and nothing after it
        try (BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8))) {
            assertThat(output.readLine()).as("lock holder output").isEqualTo("locked");
        } catch (IOException | AssertionError e) {
            process.destroyForcibly();
            throw e;
        }
        return process;
    }

    /**
     * Started in another JVM by {@link #startLockHolder}, locks the byte of a lock key in a file until its input is
     * closed
     */
    public static class LockHolder {
        // standard output tells the parent the lock is held, and the unnamed lock variable keeps its type
        @SuppressWarnings({"java:S106", "java:S7466"})
        public static void main(String[] args) throws IOException {
            try (RandomAccessFile file = new RandomAccessFile(args[0], "rw");
                    FileLock _ = file.getChannel().lock(StripingFileLock.getBucket(args[1]), 1, false)) {
                System.out.println("locked");
                System.out.flush();
                while (System.in.read() != -1) {
                    // wait for the parent to close our input
                }
            }
        }
    }

    @Test
    void reopensTheChannelWhenLockingFailsWithAnIOException() {
        FailingProvider provider = new FailingProvider(locksFile);
        provider.lockFailures.set(1);
        Lock lock = provider.acquire("reopensTheChannelWhenLockingFails");
        assertThat(provider.opened).hasSize(2);
        assertThat(provider.opened.get(0).isOpen()).isFalse();
        assertThat(((StripingFileLock) lock).isFileLockValid()).isTrue();
        lock.release();
        provider.close();
    }

    @Test
    @Timeout(10)
    void failsAtOnceWhenTheReopenedChannelFailsToo() {
        FailingProvider provider = new FailingProvider(locksFile);
        provider.lockFailures.set(Integer.MAX_VALUE);
        long start = System.nanoTime();
        assertThatThrownBy(() -> provider.acquire("failsWhenTheReopenedChannelFails"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unexpected error acquiring lock")
                .hasRootCauseMessage("Bad file descriptor");
        // not after the two minutes of attempts that a lock held by another process gets
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isLessThan(2000);
        assertThat(provider.opened).hasSize(2);
        provider.close();
    }

    @Test
    void doesNotReopenTheChannelUnderAHeldLock() {
        FailingProvider provider = new FailingProvider(locksFile);
        Lock held = provider.acquire("heldWhileTheChannelFails");
        provider.lockFailures.set(1);
        assertThatThrownBy(() -> provider.acquire("failsUnderAHeldLock"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unexpected error acquiring lock");
        assertThat(provider.opened).hasSize(1);
        assertThat(((StripingFileLock) held).isFileLockValid()).isTrue();
        held.release();
        Lock after = provider.acquire("failsUnderAHeldLock");
        after.release();
        provider.close();
    }

    @Test
    void reopensTheChannelWhenReleasingFails() {
        FailingProvider provider = new FailingProvider(locksFile);
        provider.failReleases.set(true);
        Lock lock = provider.acquire("releaseFails");
        assertThatThrownBy(lock::release)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failure while trying to release lock for key releaseFails");
        assertThat(provider.opened.get(0).isOpen()).isFalse();
        assertThat(provider.bucketsHeldForKey).isEmpty();
        provider.failReleases.set(false);
        Lock again = provider.acquire("releaseFails");
        assertThat(provider.opened).hasSize(2);
        again.release();
        provider.close();
    }

    /** Hands out channels whose lock calls fail on demand, as a channel whose handle died does */
    private static final class FailingProvider extends StripingFileLockProvider {
        final AtomicInteger lockFailures = new AtomicInteger();
        final AtomicBoolean failReleases = new AtomicBoolean();
        final List<FileChannel> opened = new ArrayList<>();

        FailingProvider(File locksFile) {
            super(locksFile);
        }

        @Override
        FileChannel openChannel(File file) {
            FileChannel channel = new FailingChannel(super.openChannel(file), lockFailures, failReleases);
            opened.add(channel);
            return channel;
        }
    }

    private static final class FailingChannel extends FileChannel {
        private final FileChannel delegate;
        private final AtomicInteger lockFailures;
        private final AtomicBoolean failReleases;

        FailingChannel(FileChannel delegate, AtomicInteger lockFailures, AtomicBoolean failReleases) {
            this.delegate = delegate;
            this.lockFailures = lockFailures;
            this.failReleases = failReleases;
        }

        @Override
        public FileLock tryLock(long position, long size, boolean shared) throws IOException {
            if (lockFailures.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0) {
                throw new IOException("Bad file descriptor");
            }
            FileLock lock = delegate.tryLock(position, size, shared);
            if (lock == null || !failReleases.get()) {
                return lock;
            }
            return new FileLock(this, position, size, shared) {
                @Override
                public boolean isValid() {
                    return lock.isValid();
                }

                @Override
                public void release() throws IOException {
                    throw new IOException("Bad file descriptor");
                }
            };
        }

        @Override
        public FileLock lock(long position, long size, boolean shared) throws IOException {
            return delegate.lock(position, size, shared);
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            return delegate.read(dst);
        }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            return delegate.read(dsts, offset, length);
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            return delegate.write(src);
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            return delegate.write(srcs, offset, length);
        }

        @Override
        public long position() throws IOException {
            return delegate.position();
        }

        @Override
        public FileChannel position(long newPosition) throws IOException {
            delegate.position(newPosition);
            return this;
        }

        @Override
        public long size() throws IOException {
            return delegate.size();
        }

        @Override
        public FileChannel truncate(long size) throws IOException {
            delegate.truncate(size);
            return this;
        }

        @Override
        public void force(boolean metaData) throws IOException {
            delegate.force(metaData);
        }

        @Override
        public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
            return delegate.transferTo(position, count, target);
        }

        @Override
        public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
            return delegate.transferFrom(src, position, count);
        }

        @Override
        public int read(ByteBuffer dst, long position) throws IOException {
            return delegate.read(dst, position);
        }

        @Override
        public int write(ByteBuffer src, long position) throws IOException {
            return delegate.write(src, position);
        }

        @Override
        public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
            return delegate.map(mode, position, size);
        }

        @Override
        protected void implCloseChannel() throws IOException {
            delegate.close();
        }
    }
}
