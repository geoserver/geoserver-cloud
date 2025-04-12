/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.locking;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.Callable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.util.OwsUtils;

/**
 * An abstract utility class providing support for cluster-wide locking of catalog and configuration operations.
 *
 * <p>This class defines a framework for executing actions within a {@link GeoServerConfigurationLock}, which in
 * GeoServer Cloud ensures cluster-wide update safety. It offers two implementations: one that enforces locking
 * ({@link Enabled}) and one that bypasses it ({@link Calling}). The locking variant uses a
 * {@link GeoServerConfigurationLock} to manage write locks, supporting both direct execution and callable tasks
 * with exception handling. Static methods provide utility functions for identifying {@link Info} objects.
 *
 * <p>Example usage:
 * <pre>
 * GeoServerConfigurationLock lock = ...;
 * LockingSupport locking = LockingSupport.locking(lock);
 * locking.runInWriteLock(() -> System.out.println("Locked action"), "example operation");
 * </pre>
 *
 * @since 1.0
 * @see GeoServerConfigurationLock
 */
@Slf4j(topic = "org.geoserver.cloud.catalog.locking")
public abstract class LockingSupport {

    /**
     * Executes a {@link Runnable} action within a cluster-wide write lock.
     *
     * @param action The action to execute; must not be null.
     * @param reason A descriptive reason for the lock, used for logging purposes; must not be null.
     */
    public abstract void runInWriteLock(Runnable action, String reason);

    /**
     * Executes a {@link Callable} action within a cluster-wide write lock, handling specified exceptions.
     *
     * @param <V> The return type of the callable.
     * @param <E> The exception type to handle.
     * @param exceptionType The class of exception to catch and rethrow; must not be null.
     * @param action The callable action to execute; must not be null.
     * @param reason A descriptive reason for the lock, used for logging purposes; must not be null.
     * @return The result of the callable action.
     * @throws E if the action throws an exception of type {@code E}.
     */
    public abstract <V, E extends Exception> V callInWriteLock(
            Class<E> exceptionType, Callable<V> action, String reason) throws E;

    /**
     * A locking implementation that uses a {@link GeoServerConfigurationLock} to ensure cluster-wide safety.
     */
    @RequiredArgsConstructor
    private static class Enabled extends LockingSupport {

        private final @NonNull GeoServerConfigurationLock configurationLock;

        /**
         * Acquires a write lock or upgrades an existing read lock, logging the action.
         *
         * @param reason The reason for acquiring the lock, used in logs.
         */
        private void lock(String reason) {
            final LockType currentLock = configurationLock.getCurrentLock();
            if (null == currentLock) {
                log.debug(" Acquiring write lock during {}", reason);
                configurationLock.lock(LockType.WRITE);
                log.debug("  Acquired write lock during {}", reason);
            } else if (currentLock == LockType.WRITE) {
                log.debug("Reentering write lock during {}", reason);
                configurationLock.lock(LockType.WRITE);
                log.debug(" Reentered write lock during {}", reason);
            } else if (currentLock == LockType.READ) {
                log.debug(" Upgrading read lock to write lock during {}", reason);
                configurationLock.tryUpgradeLock();
                log.debug("  Upgraded to write lock during {}", reason);
            }
        }

        /**
         * Releases the write lock, logging the action and handling cases where no lock is held.
         *
         * @param reason The reason for releasing the lock, used in logs.
         */
        private void unlock(String reason) {
            final LockType currentLock = configurationLock.getCurrentLock();
            if (null == currentLock) {
                log.info(" Attempted to release a lock while not holding it");
                configurationLock.unlock();
            } else {
                log.debug(" Releasing write lock after  {}", reason);
                configurationLock.unlock();
                log.debug("  Released write lock after  {}", reason);
            }
        }

        /**
         * {@inheritDoc}
         * <p>Executes the action within a cluster-wide write lock, converting checked exceptions to runtime exceptions.
         */
        public void runInWriteLock(Runnable action, String reason) {
            try {
                callInWriteLock(
                        Exception.class,
                        () -> {
                            action.run();
                            return null;
                        },
                        reason);
            } catch (Exception e) {
                if (e instanceof IOException ioe) {
                    throw new UncheckedIOException(ioe);
                }
                if (e instanceof RuntimeException rte) {
                    throw rte;
                }
                throw new IllegalStateException(e);
            }
        }

        /**
         * {@inheritDoc}
         * <p>Executes the callable within a cluster-wide write lock, handling specified exceptions and ensuring lock release.
         */
        public <V, E extends Exception> V callInWriteLock(Class<E> exceptionType, Callable<V> action, String reason)
                throws E {
            lock(reason);
            try {
                return action.call();
            } catch (Exception e) {
                if (exceptionType.isInstance(e)) {
                    throw exceptionType.cast(e);
                }
                throw new IllegalStateException(e);
            } finally {
                unlock(reason);
            }
        }
    }

    /**
     * A non-locking implementation that executes actions without synchronization.
     */
    private static class Calling extends LockingSupport {

        /**
         * {@inheritDoc}
         * <p>Executes the action directly without acquiring a lock.
         */
        @Override
        public void runInWriteLock(Runnable action, String reason) {
            action.run();
        }

        /**
         * {@inheritDoc}
         * <p>Executes the callable directly without acquiring a lock, handling specified exceptions.
         */
        @Override
        public <V, E extends Exception> V callInWriteLock(Class<E> exceptionType, Callable<V> action, String reason)
                throws E {
            try {
                return action.call();
            } catch (Exception e) {
                if (exceptionType.isInstance(e)) {
                    throw exceptionType.cast(e);
                }
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Extracts a the name property from an {@link Info} object.
     *
     * <p>For {@link SettingsInfo}, uses the workspace name if available; otherwise, retrieves the "name" or "prefix"
     * property, falling back to the "id" or type name if unset.
     *
     * @param object The {@link Info} object; may be null.
     * @return The extracted name, or null if {@code object} is null.
     */
    public static String nameOf(Info object) {
        if (null == object) {
            return null;
        }
        if (object instanceof SettingsInfo settings) {
            WorkspaceInfo ws = settings.getWorkspace();
            if (ws != null) {
                return ws.getName();
            }
        }
        String property = object instanceof NamespaceInfo ? "prefix" : "name";
        String name = null;
        if (OwsUtils.has(ModificationProxy.unwrap(object), property)) {
            name = (String) OwsUtils.get(object, property);
            if (null == name) {
                name = (String) OwsUtils.get(object, "id");
            }
        }
        return null == name ? ConfigInfoType.valueOf(object).name() : name;
    }

    /**
     * Determines the {@link ConfigInfoType} of an {@link Info} object.
     *
     * @param object The {@link Info} object; may be null.
     * @return The corresponding {@link ConfigInfoType}, or null if {@code object} is null.
     */
    public static ConfigInfoType typeOf(Info object) {
        if (null == object) {
            return null;
        }
        return ConfigInfoType.valueOf(object);
    }

    /**
     * Creates a locking support instance that enforces cluster-wide safety.
     *
     * @param configurationLock The {@link GeoServerConfigurationLock} to use; must not be null.
     * @return A locking-enabled {@link LockingSupport} instance.
     * @throws NullPointerException if {@code configurationLock} is null.
     */
    public static LockingSupport locking(@NonNull GeoServerConfigurationLock configurationLock) {
        return new Enabled(configurationLock);
    }

    /**
     * Creates a locking support instance that bypasses locking.
     *
     * @return A non-locking {@link LockingSupport} instance.
     */
    public static LockingSupport ignoringLocking() {
        return new Calling();
    }
}
