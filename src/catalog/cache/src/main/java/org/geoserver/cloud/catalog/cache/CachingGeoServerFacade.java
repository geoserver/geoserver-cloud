/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.cache;

import lombok.NonNull;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.springframework.cache.CacheManager;

import java.util.Optional;

import javax.annotation.Nullable;

/**
 * Extension of {@link GeoServerFacade} to signify some methods cache or evict {@link Info} objects.
 *
 * <p>Expected implementation uses spring-cache annotations on selected methods, and/or manual cache
 * handling for example, to evict multiple keys
 *
 * @see CachingGeoServerFacadeImpl
 */
public interface CachingGeoServerFacade extends GeoServerFacade {

    /**
     * Name of the cache used by {@link CachingGeoServerFacadeImpl}, used as key to acquire it
     * through {@link CacheManager#getCache(String)}
     */
    String CACHE_NAME = "gs-config";
    /** Key used to cache and evict the {@link GeoServerInfo global configuration} object */
    String GEOSERVERINFO_KEY = "global_GeoServer";
    /** Key used to cache and evict the {@link LoggingInfo global logging} settings object */
    String LOGGINGINFO_KEY = "global_Logging";

    /**
     * Method used to build a cache key for the {@link SettingsInfo settings} of a given workspace
     */
    public static Object settingsKey(WorkspaceInfo ws) {
        return "settings@" + ws.getId();
    }

    public static Object serviceByIdKey(String id) {
        return ServiceInfoKey.byId(id);
    }

    public static Object serviceByNameKey(@Nullable WorkspaceInfo ws, @NonNull String name) {
        return ServiceInfoKey.byName(ws, name);
    }

    public static Object serviceByTypeKey(
            @Nullable WorkspaceInfo ws, @NonNull Class<? extends ServiceInfo> type) {
        return ServiceInfoKey.byType(ws, type);
    }

    /**
     * Evicts the given object from the cache
     *
     * @param info a {@link CatalogInfo} object to evict, can be a proxy as long as it provides a
     *     proper {@link Info#getId() id}
     */
    boolean evict(Info info);

    /**
     * Evicts the {@link GeoServerInfo} if cached
     *
     * @return the cached value or empty
     */
    Optional<GeoServerInfo> evictGlobal();
}
