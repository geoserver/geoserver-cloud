/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.repository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.gwc.layer.TileLayerCatalogListener;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueRetrievalException;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @since 1.0
 */
@RequiredArgsConstructor
public class CachingTileLayerCatalog implements TileLayerCatalog {

    private static final String TILE_LAYERS_BY_ID = "TILE_LAYERS_BY_ID";
    private static final String TILE_LAYERS_BY_NAME = "TILE_LAYERS_BY_NAME";

    private final CacheManager cacheManager;
    private final ResourceStoreTileLayerCatalog delegate;

    private Cache idCache;
    private Cache nameCache;
    private ConcurrentMap<String, String> namesById;

    @EventListener(TileLayerEvent.class)
    public void onTileLayerEvent(TileLayerEvent event) {
        switch (event.getEventType()) {
            case CREATED:
                getLayerById(event.getPublishedId());
                break;
            case DELETED:
                evictById(event.getPublishedId());
                break;
            case MODIFIED:
                evictById(event.getPublishedId());
                getLayerById(event.getPublishedId());
                break;
            default:
                throw new IllegalArgumentException(
                        "Invalid TileLayerEvent type: " + event.getEventType());
        }
    }

    public void evictById(@NonNull String id) {
        final String name = namesById.remove(id);
        idCache.evict(id);
        if (name != null) {
            nameCache.evict(name);
        }
    }

    @Override
    public synchronized void initialize() {
        delegate.initialize();
        idCache = cacheManager.getCache(TILE_LAYERS_BY_ID);
        nameCache = cacheManager.getCache(TILE_LAYERS_BY_NAME);
        namesById = new ConcurrentHashMap<>();
        preLoad();
    }

    @Override
    public synchronized void reset() {
        if (idCache != null) {
            idCache.clear();
            idCache = null;
        }
        if (nameCache != null) {
            nameCache.clear();
            nameCache = null;
        }
        if (namesById != null) {
            namesById.clear();
            namesById = null;
        }
        delegate.reset();
    }

    private void preLoad() {
        delegate.findAll().forEach(this::onLoaded);
    }

    private void onLoaded(@NonNull GeoServerTileLayerInfo info) {
        idCache.put(info.getId(), info);
        nameCache.put(info.getName(), info);
        cacheIdentifiers(info);
    }

    private void cacheIdentifiers(@NonNull GeoServerTileLayerInfo info) {
        namesById.put(info.getId(), info.getName());
    }

    @Override
    public void addListener(TileLayerCatalogListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public Set<String> getLayerIds() {
        return new HashSet<>(namesById.keySet());
    }

    @Override
    public Set<String> getLayerNames() {
        return new HashSet<>(namesById.values());
    }

    @Override
    public String getLayerId(@NonNull String layerName) {
        GeoServerTileLayerInfo layer = getLayerByName(layerName);
        return layer == null ? null : layer.getId();
    }

    @Override
    public String getLayerName(@NonNull String layerId) {
        return namesById.get(layerId);
    }

    @Override
    public GeoServerTileLayerInfo getLayerById(@NonNull String id) {
        try {
            return idCache.get(id, () -> loadLayerById(id));
        } catch (ValueRetrievalException e) {
            if (e.getCause() instanceof NoSuchElementException) return null;
            throw e;
        }
    }

    @Override
    public GeoServerTileLayerInfo getLayerByName(@NonNull String layerName) {
        try {
            return nameCache.get(layerName, () -> loadLayerByName(layerName));
        } catch (ValueRetrievalException e) {
            if (e.getCause() instanceof NoSuchElementException) return null;
            throw e;
        }
    }

    private GeoServerTileLayerInfo loadLayerById(String id) {
        GeoServerTileLayerInfo info = delegate.getLayerById(id);
        if (info == null) {
            throw new NoSuchElementException(id);
        }
        cacheIdentifiers(info);
        return info;
    }

    private GeoServerTileLayerInfo loadLayerByName(String name) {
        GeoServerTileLayerInfo info = delegate.getLayerByName(name);
        if (info == null) {
            throw new NoSuchElementException(name);
        }
        cacheIdentifiers(info);
        return info;
    }

    @Override
    public GeoServerTileLayerInfo delete(@NonNull String tileLayerId) {
        return delegate.delete(tileLayerId);
    }

    @Override
    public GeoServerTileLayerInfo save(@NonNull GeoServerTileLayerInfo newValue) {
        return delegate.save(newValue);
    }

    @Override
    public boolean exists(@NonNull String layerId) {
        return delegate.exists(layerId);
    }

    @Override
    public String getPersistenceLocation() {
        return delegate.getPersistenceLocation();
    }
}
