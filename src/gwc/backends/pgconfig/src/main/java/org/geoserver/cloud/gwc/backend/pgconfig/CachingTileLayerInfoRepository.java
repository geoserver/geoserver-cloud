/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.backend.pgconfig;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.springframework.cache.Cache;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@link TileLayerInfoRepository} decorator cache {@link TileLayerInfo}s on demand, alleviating the
 * load on the delegate, especially under load.
 */
@RequiredArgsConstructor
@Slf4j(topic = "org.geoserver.cloud.gwc.backend.pgconfig.caching")
public class CachingTileLayerInfoRepository implements TileLayerInfoRepository {

    public static final String CACHE_NAME = "gwc-tilelayerinfo";

    private final @NonNull TileLayerInfoRepository repository;
    private final @NonNull Cache nameCache;

    /** cached value for {@link #findAllNames()}, cleared upon any {@link TileLayerEvent} */
    private Set<String> cachedNames;

    @EventListener(TileLayerEvent.class)
    void onTileLayerEvent(TileLayerEvent event) {
        cachedNames = null;
        log.debug("handling {}", event);
        String prefixedName = null == event.getOldName() ? event.getName() : event.getOldName();
        evict(prefixedName);
    }

    static record NameKey(String worksapce, @NonNull String layer) implements Serializable {
        @Override
        public String toString() {
            return worksapce() == null ? layer() : "%s:%s".formatted(worksapce(), layer());
        }

        public static NameKey valueOf(@NonNull String prefixedName) {
            String workspace = InfoEvent.prefix(prefixedName).orElse(null);
            String name = InfoEvent.localName(prefixedName);
            return new NameKey(workspace, name);
        }
    }

    private void cache(@NonNull TileLayerInfo tli) {
        NameKey key = name(tli);
        nameCache.put(key, tli);
        log.debug("cached {}", key);
    }

    private void evict(String prefixedName) {
        evict(NameKey.valueOf(prefixedName));
    }

    private void evict(NameKey key) {
        if (nameCache.evictIfPresent(key)) {
            log.debug("evicted {}", key);
        }
    }

    private void evict(@NonNull TileLayerInfo tli) {
        evict(name(tli));
    }

    private NameKey name(TileLayerInfo tli) {
        String ws = tli.workspace().orElse(null);
        String name = tli.name();
        return name(ws, name);
    }

    private NameKey name(String ws, String name) {
        return new NameKey(ws, name);
    }

    @Override
    public void add(@NonNull TileLayerInfo tli) throws DataAccessException {
        repository.add(tli);
        cache(tli);
    }

    @Override
    public boolean save(@NonNull TileLayerInfo tli) throws DataAccessException {
        try {
            evict(tli);
            boolean updated = repository.save(tli);
            if (updated) cache(tli);
            return updated;
        } catch (RuntimeException e) {
            evict(tli);
            throw e;
        }
    }

    @Override
    public boolean delete(@Nullable String workspace, @NonNull String name)
            throws DataAccessException {

        evict(name(workspace, name));
        return repository.delete(workspace, name);
    }

    @Override
    public Stream<TileLayerInfo> findAll() throws DataAccessException {
        return repository.findAll();
    }

    @Override
    public Optional<TileLayerInfo> find(@Nullable String workspace, @NonNull String layer)
            throws DataAccessException {

        NameKey key = name(workspace, layer);
        return Optional.ofNullable(nameCache.get(key, () -> load(workspace, layer)));
    }

    private TileLayerInfo load(String workspace, String layer) {
        log.debug("loading layer {}", name(workspace, layer));
        return repository.find(workspace, layer).orElse(null);
    }

    private Optional<TileLayerInfo> findCached(@Nullable String workspace, @NonNull String layer) {
        NameKey name = name(workspace, layer);
        return Optional.ofNullable(nameCache.get(name, TileLayerInfo.class));
    }

    @Override
    public int count() throws DataAccessException {
        return repository.count();
    }

    @Override
    public Set<String> findAllNames() throws DataAccessException {
        Set<String> allNames = this.cachedNames;
        if (null == allNames) {
            allNames = repository.findAllNames();
            this.cachedNames = Set.copyOf(allNames);
        }
        return allNames;
    }

    @Override
    public boolean exists(String workspace, @NonNull String layer) throws DataAccessException {
        return findCached(workspace, layer)
                .map(
                        tl -> {
                            log.trace(
                                    "returning exists=true from cache for layer {}",
                                    name(workspace, layer));
                            return true;
                        })
                .orElseGet(() -> repository.exists(workspace, layer));
    }
}
