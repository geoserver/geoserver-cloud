/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gwc.backend.pgconfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import org.geoserver.catalog.plugin.requestcache.RequestCache;
import org.geoserver.catalog.plugin.requestcache.RequestCacheScope;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataAccessException;

/**
 * Keeps the GWC settings found by {@link #findSettings(String)} and {@link #findAllSettings(Collection)} in the cache
 * of the current request, as the catalog keeps the objects of the request. A tile request looks its tile layer up more
 * than once, and decodes the settings once. A request listing the tile layers several times decodes them once too. A
 * layer or group without tile layer is kept as a miss.
 *
 * <p>The writes evict the settings from the cache of the request: a request reads its own writes. Outside a request,
 * and once the request ended, each call goes to the decorated repository.
 *
 * <p>The lookups and listings of a request share the cached settings. The tile layer catalog only reads them, and
 * builds a new info from them for each returned tile layer.
 */
public class RequestCachingTileLayerInfoRepository implements TileLayerInfoRepository {

    /** The key of the settings of a layer or group in the cache of a request. */
    record SettingsKey(String publishedId) {}

    private final TileLayerInfoRepository delegate;

    public RequestCachingTileLayerInfoRepository(@NonNull TileLayerInfoRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<TileLayerInfo> findSettings(@NonNull String publishedId) throws DataAccessException {
        RequestCache requestCache = liveRequestCache();
        if (null == requestCache) {
            return delegate.findSettings(publishedId);
        }
        SettingsKey key = new SettingsKey(publishedId);
        Optional<Object> cached = requestCache.found(key);
        if (cached.isPresent()) {
            return settingsOf(cached.get());
        }
        Optional<TileLayerInfo> found = delegate.findSettings(publishedId);
        requestCache.put(key, found.orElse(null));
        return found;
    }

    /** The cache of the request of this thread, or null outside a request and after its end. */
    private static @Nullable RequestCache liveRequestCache() {
        RequestCache requestCache = RequestCacheScope.current();
        if (null == requestCache || requestCache.isDead()) {
            return null;
        }
        return requestCache;
    }

    /** The settings held by the cache of the request, empty for a miss. */
    private static Optional<TileLayerInfo> settingsOf(Object cached) {
        if (cached == RequestCache.MISS) {
            return Optional.empty();
        }
        return Optional.of((TileLayerInfo) cached);
    }

    /**
     * Answers the ids held by the cache of the request, and asks the decorated repository for the others with one call.
     * The ids without settings in its answer are kept as misses.
     */
    @Override
    public Map<String, TileLayerInfo> findAllSettings(@NonNull Collection<String> publishedIds)
            throws DataAccessException {
        RequestCache requestCache = liveRequestCache();
        if (null == requestCache) {
            return delegate.findAllSettings(publishedIds);
        }
        CachedSettings cached = cachedSettings(requestCache, publishedIds);
        Map<String, TileLayerInfo> found = new HashMap<>(cached.found());
        found.putAll(findAndKeep(requestCache, cached.uncached()));
        return found;
    }

    /** The answers of the cache of the request for several ids: the settings found, and the ids not cached. */
    private record CachedSettings(Map<String, TileLayerInfo> found, List<String> uncached) {}

    private static CachedSettings cachedSettings(RequestCache requestCache, Collection<String> publishedIds) {
        Map<String, TileLayerInfo> found = new HashMap<>();
        List<String> uncached = new ArrayList<>();
        for (String publishedId : publishedIds) {
            Optional<Object> cached = requestCache.found(new SettingsKey(publishedId));
            if (cached.isEmpty()) {
                uncached.add(publishedId);
            } else {
                settingsOf(cached.get()).ifPresent(settings -> found.put(publishedId, settings));
            }
        }
        return new CachedSettings(found, uncached);
    }

    /** Asks the decorated repository for the settings, and keeps its answer for each id, or a miss. */
    private Map<String, TileLayerInfo> findAndKeep(RequestCache requestCache, List<String> publishedIds) {
        if (publishedIds.isEmpty()) {
            return Map.of();
        }
        Map<String, TileLayerInfo> found = delegate.findAllSettings(publishedIds);
        for (String publishedId : publishedIds) {
            requestCache.put(new SettingsKey(publishedId), found.get(publishedId));
        }
        return found;
    }

    @Override
    public boolean save(@NonNull String publishedId, @NonNull TileLayerInfo settings) throws DataAccessException {
        boolean saved = delegate.save(publishedId, settings);
        evictFromRequest(publishedId);
        return saved;
    }

    @Override
    public boolean delete(@NonNull String publishedId) throws DataAccessException {
        boolean deleted = delegate.delete(publishedId);
        evictFromRequest(publishedId);
        return deleted;
    }

    private static void evictFromRequest(String publishedId) {
        RequestCache requestCache = liveRequestCache();
        if (null != requestCache) {
            requestCache.evict(new SettingsKey(publishedId));
        }
    }

    @Override
    public List<String> findAllIds() throws DataAccessException {
        return delegate.findAllIds();
    }

    @Override
    public int count() throws DataAccessException {
        return delegate.count();
    }

    @Override
    public Set<String> findAllNames() throws DataAccessException {
        return delegate.findAllNames();
    }
}
