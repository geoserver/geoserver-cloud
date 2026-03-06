/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/**
 * Distributed locking for multi-pod shared data directory access.
 *
 * <p>In GeoServer Cloud, multiple pods share the same data directory via a mounted filesystem. The upstream
 * {@code FileLockProvider} is <b>unsafe</b> for this scenario because it creates a new file per lock key and deletes it
 * on release, introducing a race condition: when one process releases and deletes the lock file, another process
 * waiting on a {@code FileOutputStream} it already opened succeeds on a phantom file descriptor, while a third process
 * creates a fresh file and also acquires a lock — resulting in two processes believing they hold exclusive access.
 *
 * <h2>Solution</h2>
 *
 * <p>This package uses a <b>single persistent file</b> ({@code <datadir>/.filelocks/resourcestore.locks}) that is never
 * deleted, with <b>byte-range striping</b>: each lock key is SHA-1 hashed to one of 1M buckets, and the lock is a
 * {@link java.nio.channels.FileLock} on a 1-byte region at that bucket's offset. Because all processes share the same
 * file, NIO file locks correctly arbitrate between them.
 *
 * <p>Since NIO {@link java.nio.channels.FileLock} does not prevent same-JVM threads from acquiring overlapping regions
 * via different channels, an in-JVM {@code MemoryLockProvider} is chained <i>before</i> the file-based provider to
 * synchronize threads within the same process.
 *
 * <h2>Class hierarchy</h2>
 *
 * <ul>
 *   <li>{@link org.geoserver.cloud.catalog.backend.datadir.locking.DistributedFileLockProvider} — public entry point /
 *       Spring bean; creates and chains the memory and file lock providers.
 *   <li>{@link org.geoserver.cloud.catalog.backend.datadir.locking.ChainedLockProvider} — acquires locks from two
 *       providers sequentially, releases in reverse order.
 *   <li>{@link org.geoserver.cloud.catalog.backend.datadir.locking.StripingFileLockProvider} — manages the shared lock
 *       file and creates {@code StripingFileLock} instances per key.
 *   <li>{@link org.geoserver.cloud.catalog.backend.datadir.locking.StripingFileLock} — represents a single held lock;
 *       handles byte-range acquisition, reentrancy, and collision detection.
 * </ul>
 */
package org.geoserver.cloud.catalog.backend.datadir.locking;
