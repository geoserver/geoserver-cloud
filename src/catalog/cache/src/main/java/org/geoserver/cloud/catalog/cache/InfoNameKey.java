/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.cache;

import lombok.NonNull;

import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.springframework.cache.interceptor.SimpleKey;

import java.io.Serializable;
import java.util.Optional;

/**
 * A key to for a cached {@link Info} using its prefixed name, according to {@link
 * InfoEvent#prefixedName(Info)}.
 *
 * <p>easier than implementing multiple key generators; it's also a smaller memory footprint than
 * {@link SimpleKey}
 */
record InfoNameKey(@NonNull String prefixexName, @NonNull ConfigInfoType type)
        implements Serializable {

    public static InfoNameKey valueOf(@NonNull Info info) {
        return new InfoNameKey(InfoEvent.prefixedName(info), InfoEvent.typeOf(info));
    }

    public static InfoNameKey valueOf(
            WorkspaceInfo workspace, @NonNull String name, ConfigInfoType type) {

        if (CatalogFacade.NO_WORKSPACE == workspace) {
            return valueOf(name, type);
        }

        return valueOf(InfoEvent.prefixedName(workspace, name), type);
    }

    public static InfoNameKey valueOf(@NonNull String name, ConfigInfoType type) {
        return new InfoNameKey(name, type);
    }

    public static InfoNameKey valueOf(
            NamespaceInfo namespace, @NonNull String name, @NonNull Class<? extends Info> clazz) {

        return valueOf(InfoEvent.prefixedName(namespace, name), ConfigInfoType.valueOf(clazz));
    }

    public InfoNameKey withLocalName(String localName) {
        Optional<String> prefix = InfoEvent.prefix(prefixexName());
        var newPrefixedName = InfoEvent.prefixedName(prefix, localName);
        return new InfoNameKey(newPrefixedName, type());
    }
}
