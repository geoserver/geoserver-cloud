/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.cache;

import lombok.NonNull;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.springframework.cache.interceptor.SimpleKey;

import java.io.Serializable;

/**
 * A key to for a cached {@link Info} using its {@link Info#getId() id}.
 *
 * <p>easier than implementing multiple key generators; it's also a smaller memory footprint than
 * {@link SimpleKey}
 */
record InfoIdKey(@NonNull String id, @NonNull ConfigInfoType type) implements Serializable {

    /**
     * Caching constructor, resolves {@code info}'s type to its concrete {@link
     * ClassMappings#getInterface()}
     */
    public static InfoIdKey valueOf(@NonNull Info info) {
        String id = InfoEvent.resolveId(info);
        ConfigInfoType type = InfoEvent.typeOf(info);
        return valueOf(id, type);
    }

    public static InfoIdKey valueOf(@NonNull String id, @NonNull ConfigInfoType type) {
        return new InfoIdKey(id, type);
    }

    public static InfoIdKey valueOf(@NonNull String id, @NonNull Class<? extends Info> clazz) {
        return valueOf(id, ConfigInfoType.valueOf(clazz));
    }
}
