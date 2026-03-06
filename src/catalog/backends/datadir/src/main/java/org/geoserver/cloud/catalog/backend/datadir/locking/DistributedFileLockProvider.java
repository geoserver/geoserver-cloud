/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.catalog.backend.datadir.locking;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.logging.Logger;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.MemoryLockProvider;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.DisposableBean;

/**
 * Distributed lock provider for multi-pod shared data directory access.
 *
 * <p>This is the public entry point and Spring bean. It implements a two-level locking strategy by chaining an in-JVM
 * {@link org.geoserver.platform.resource.MemoryLockProvider} with a {@link StripingFileLockProvider} via
 * {@link ChainedLockProvider}:
 *
 * <ol>
 *   <li><b>Memory lock</b> (acquired first) — synchronizes threads within the same JVM, since NIO
 *       {@link java.nio.channels.FileLock} does not prevent same-JVM threads from acquiring overlapping regions via
 *       different channels.
 *   <li><b>File lock</b> (acquired second) — synchronizes across processes/pods using byte-range locking on a single
 *       shared file.
 * </ol>
 *
 * <p>The lock file is located at {@code <datadir>/.filelocks/resourcestore.locks} and is created automatically on
 * construction if it does not exist.
 *
 * <p>Implements {@link DisposableBean} to close the underlying file channel on Spring context shutdown.
 *
 * @see ChainedLockProvider
 * @see StripingFileLockProvider
 */
public class DistributedFileLockProvider implements LockProvider, DisposableBean {

    static final Logger LOGGER = Logging.getLogger(DistributedFileLockProvider.class);

    private ChainedLockProvider delegate;
    private StripingFileLockProvider fileLockProvider;

    private File root;

    public DistributedFileLockProvider(File basePath) {
        this.root = Objects.requireNonNull(basePath);
        // first off, synchronize among threads in the same jvm (the nio locks won't lock threads in the same JVM)
        LockProvider memory = new MemoryLockProvider();
        // then synch up between different processes
        this.fileLockProvider = new StripingFileLockProvider(getLocksFile());
        this.delegate = new ChainedLockProvider(memory, fileLockProvider);
    }

    @Override
    public Resource.Lock acquire(final String lockKey) {
        return delegate.acquire(lockKey);
    }

    @Override
    public void destroy() throws Exception {
        StripingFileLockProvider flp = this.fileLockProvider;
        this.fileLockProvider = null;
        if (flp != null) {
            flp.close();
        }
    }

    private File getLocksFile() {
        Objects.requireNonNull(this.root, "Root directory not set");
        File locksDir = new File(this.root, ".filelocks");
        if (locksDir.isFile()) {
            throw new IllegalStateException(
                    "locks directory %s exists but it's a file".formatted(locksDir.getAbsolutePath()));
        }
        if (locksDir.mkdirs()) {
            LOGGER.fine(() -> "Created locks directory " + locksDir);
        } else if (!locksDir.isDirectory()) {
            throw new IllegalStateException(
                    "%s is not a directory or can't be created".formatted(locksDir.getAbsolutePath()));
        }

        File file = new File(locksDir, "resourcestore.locks");
        try {
            if (file.createNewFile()) {
                LOGGER.fine(() -> "Created locks file %s".formatted(file.getAbsolutePath()));
            } else if (!file.isFile()) {
                throw new IllegalStateException(
                        "%s is not a file or can't be created".formatted(locksDir.getAbsolutePath()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Error creating locks file " + file.getAbsolutePath(), e);
        }

        return file;
    }

    @Override
    public String toString() {
        return "FileLockProvider " + root;
    }
}
