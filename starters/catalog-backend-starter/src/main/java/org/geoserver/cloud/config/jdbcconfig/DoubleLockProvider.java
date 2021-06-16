/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.jdbcconfig;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.Resource.Lock;

/** */
@RequiredArgsConstructor
public class DoubleLockProvider implements LockProvider {

    private final @NonNull LockProvider first;
    private final @NonNull LockProvider second;

    @Override
    public Lock acquire(@NonNull String path) {
        Lock lock1 = first.acquire(path);
        try {
            Lock lock2 = second.acquire(path);
            return new DoubleLock(lock1, lock2);
        } catch (RuntimeException e) {
            lock1.release();
            throw e;
        }
    }

    public @Override String toString() {
        return String.format(
                "%s[%s,%s]",
                getClass().getSimpleName(),
                first.getClass().getSimpleName(),
                second.getClass().getSimpleName());
    }

    @RequiredArgsConstructor
    private static class DoubleLock implements Lock {

        private final @NonNull Lock first;
        private final @NonNull Lock second;

        @Override
        public void release() {
            try {
                second.release();
            } finally {
                first.release();
            }
        }
    }
}
