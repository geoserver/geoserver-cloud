/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.platform.config;

import lombok.NonNull;

import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @since 1.0
 */
public class DefaultUpdateSequence implements UpdateSequence {

    private static final long UNSET = Long.MIN_VALUE;

    private final AtomicLong sequence = new AtomicLong(UNSET);

    private final Lock lock = new ReentrantLock();

    private final GeoServer geoServer;

    public DefaultUpdateSequence(@NonNull GeoServer gs) {
        this.geoServer = gs;
        sequence.set(
                Optional.ofNullable(gs.getGlobal())
                        .map(GeoServerInfo::getUpdateSequence)
                        .orElse(0L));
    }

    public @Override long currValue() {
        return info().map(GeoServerInfo::getUpdateSequence).orElse(0L);
    }

    public @Override long nextValue() {
        lock.lock();
        try {
            GeoServerInfo global = info().orElse(null);
            if (global == null) return 0;
            long nextVal = sequence.incrementAndGet();
            if (global != null) {
                global = ModificationProxy.unwrap(global);
                global.setUpdateSequence(nextVal);
            }
            return nextVal;
        } finally {
            lock.unlock();
        }
    }

    private Optional<GeoServerInfo> info() {
        return Optional.ofNullable(geoServer.getGlobal())
                .map(
                        global -> {
                            sequence.compareAndSet(UNSET, global.getUpdateSequence());
                            return global;
                        });
    }
}
