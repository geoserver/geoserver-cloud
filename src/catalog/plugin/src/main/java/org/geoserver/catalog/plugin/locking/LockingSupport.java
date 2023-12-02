/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.locking;

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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.Callable;

/** */
@Slf4j(topic = "org.geoserver.cloud.catalog.locking")
public abstract class LockingSupport {

    public abstract void runInWriteLock(Runnable action, String reason);

    public abstract <V, E extends Exception> V callInWriteLock(
            Class<E> exceptionType, Callable<V> action, String reason) throws E;

    @RequiredArgsConstructor
    private static class Enabled extends LockingSupport {

        private final @NonNull GeoServerConfigurationLock configurationLock;

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
                if (e instanceof IOException) throw new UncheckedIOException((IOException) e);
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new RuntimeException(e);
            }
        }

        public <V, E extends Exception> V callInWriteLock(
                Class<E> exceptionType, Callable<V> action, String reason) throws E {
            lock(reason);
            try {
                return action.call();
            } catch (Exception e) {
                if (exceptionType.isInstance(e)) throw exceptionType.cast(e);
                throw new RuntimeException(e);
            } finally {
                unlock(reason);
            }
        }
    }

    private static class Calling extends LockingSupport {

        @Override
        public void runInWriteLock(Runnable action, String reason) {
            action.run();
        }

        @Override
        public <V, E extends Exception> V callInWriteLock(
                Class<E> exceptionType, Callable<V> action, String reason) throws E {

            try {
                return action.call();
            } catch (Exception e) {
                if (exceptionType.isInstance(e)) throw exceptionType.cast(e);
                throw new RuntimeException(e);
            }
        }
    }

    public static String nameOf(Info object) {
        if (null == object) return null;
        if (object instanceof SettingsInfo settings) {
            WorkspaceInfo ws = settings.getWorkspace();
            if (ws != null) return ws.getName();
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

    public static ConfigInfoType typeOf(Info object) {
        if (null == object) return null;
        return ConfigInfoType.valueOf(object);
    }

    public static LockingSupport locking(@NonNull GeoServerConfigurationLock configurationLock) {
        return new Enabled(configurationLock);
    }

    public static LockingSupport ignoringLocking() {
        return new Calling();
    }
}
