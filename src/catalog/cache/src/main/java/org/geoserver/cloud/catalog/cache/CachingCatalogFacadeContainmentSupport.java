/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.cache;

import static org.geoserver.cloud.event.info.ConfigInfoType.RESOURCE;
import static org.geoserver.cloud.event.info.ConfigInfoType.STORE;

import com.google.common.annotations.VisibleForTesting;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.AbstractCatalogVisitor;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @since 1.7
 */
@ParametersAreNonnullByDefault
@Slf4j(topic = "org.geoserver.cloud.catalog.cache")
class CachingCatalogFacadeContainmentSupport {
    /** Key used to cache and evict {@link CatalogFacade#getDefaultNamespace() default namespace} */
    static final String DEFAULT_NAMESPACE_CACHE_KEY = "defaultNamespace";

    /** Key used to cache and evict {@link CatalogFacade#getDefaultWorkspace() default workspace} */
    static final String DEFAULT_WORKSPACE_CACHE_KEY = "defaultWorkspace";

    /**
     * Prefix used to build a per-workspace-id default datastore cache key
     *
     * @see #generateDefaultDataStoreKey
     */
    static final String DEFAULT_DATASTORE_CACHE_KEY_PREFIX = "defaultDataStore@";

    @Getter(value = AccessLevel.PACKAGE)
    private final @NonNull Cache cache;

    /**
     * Maintains a list of referred objects at {@link #put(Callable)} and clears referenced objects
     * at {@link #evict(String, String, ConfigInfoType)}, so there are no cached objects holding
     * stale references to evicted objects
     */
    private final ResourceContainmentCache referencesCache;

    public CachingCatalogFacadeContainmentSupport(@NonNull Cache cache) {
        this.cache = cache;
        this.referencesCache = new ResourceContainmentCache();
    }

    @VisibleForTesting
    CachingCatalogFacadeContainmentSupport() {
        this(newCache());
    }

    private static @NonNull Cache newCache() {
        CaffeineCacheManager manager = new CaffeineCacheManager(CachingCatalogFacade.CACHE_NAME);
        return Objects.requireNonNull(manager.getCache(CachingCatalogFacade.CACHE_NAME));
    }

    /**
     * Evicts the {@link InfoIdKey} entry used for id lookups and the {@link InfoNameKey} used for
     * name lookups.
     *
     * <p>For {@link ResourceInfo} concrete-types, also evicts the entries for {@link
     * ConfigInfoType#RESOURCE}, accounting for {@link #getResource(String, Class) getResource(id,
     * ResourceInfo.class)} cached entries.
     *
     * <p>For {@link StoreInfo} concrete-types, also evicts the entries for {@link
     * ConfigInfoType#STORE}, accounting for {@link #getStore(String, Class) getStore(id,
     * StoreInfo.class)} cached entries.
     *
     * <p>For {@link ResourceInfo} and {@link PublishedInfo} sub-types, also evicts any {@link
     * LayerGroupInfo} key directly or indirectly referencing it.
     *
     * <p>For {@link StyleInfo}, also evict the entries for any {@link LayerInfo} and {@link
     * LayerGroupInfo} referencing the style.
     */
    public boolean evict(String id, String prefixedName, ConfigInfoType type) {
        return evictInternal(id, prefixedName, type, true);
    }

    private boolean evictInternal(
            String id, String prefixedName, ConfigInfoType type, boolean doLog) {
        boolean evicted = false;
        if (type.isA(WorkspaceInfo.class)) {
            evictDefaultDataStore(id, prefixedName);
        } else if (type.isA(StoreInfo.class) && type != STORE) {
            evicted |= evict(id, prefixedName, STORE);
        } else if (type.isA(ResourceInfo.class) && type != RESOURCE) {
            evicted |= evict(id, prefixedName, RESOURCE);
        }

        InfoIdKey idKey = new InfoIdKey(id, type);
        InfoNameKey nameKey = new InfoNameKey(prefixedName, type);

        evicted |= evict(idKey);
        evicted |= evict(nameKey);
        if (evicted && doLog)
            log.debug(
                    "evicted {}[id: {}, name: {}]", type.type().getSimpleName(), id, prefixedName);
        referencesCache.evict(id, prefixedName, type);
        return evicted;
    }

    boolean evict(Object key) {
        boolean evicted = cache.evictIfPresent(key);
        if (evicted) {
            log.trace("evicted {}", key);
        }
        return evicted;
    }

    public boolean evict(CatalogInfo info) {
        if (info instanceof LayerInfo l) {
            evict(generateLayersByResourceKey(l.getResource()));
        }
        return evict(info.getId(), InfoEvent.prefixedName(info), InfoEvent.typeOf(info));
    }

    public <T extends CatalogInfo> T evictAndGet(T info, Callable<T> loader) {
        evict(info);
        return put(loader);
    }

    @SneakyThrows
    <T extends CatalogInfo> T put(Callable<T> supplier) {
        T info = supplier.call();
        if (null != info) {
            put(InfoIdKey.valueOf(info), info);
            put(InfoNameKey.valueOf(info), info);
            referencesCache.put(info);
        }
        return info;
    }

    private void put(Object key, Object value) {
        cache.put(key, value);
    }

    public <T> T get(Object key, Callable<T> loader) {
        T value = cache.get(key, loader);
        if (null == value || (value instanceof Collection<?> c && c.isEmpty())) cache.evict(key);
        return value;
    }

    public List<LayerInfo> getLayersByResource(
            String resourceInfoId, Callable<List<LayerInfo>> loader) {
        InfoIdKey key = generateLayersByResourceKey(resourceInfoId);
        return get(key, loader);
    }

    Object generateDefaultDataStoreKey(String workspaceId) {
        return DEFAULT_DATASTORE_CACHE_KEY_PREFIX + workspaceId;
    }

    InfoIdKey generateLayersByResourceKey(ResourceInfo resource) {
        return generateLayersByResourceKey(resource.getId());
    }

    InfoIdKey generateLayersByResourceKey(String resourceId) {
        return new InfoIdKey("layers@" + resourceId, ConfigInfoType.LAYER);
    }

    public WorkspaceInfo getDefaultWorkspace(Callable<WorkspaceInfo> loader) {
        return cache.get(DEFAULT_WORKSPACE_CACHE_KEY, loader);
    }

    public void evictDefaultWorkspace() {
        if (evict(DEFAULT_WORKSPACE_CACHE_KEY)) {
            log.trace("default workspace evicted");
        }
    }

    public NamespaceInfo getDefaultNamespace(Callable<NamespaceInfo> loader) {
        return cache.get(DEFAULT_NAMESPACE_CACHE_KEY, loader);
    }

    public void evictDefaultNamespace() {
        if (evict(DEFAULT_NAMESPACE_CACHE_KEY)) {
            log.trace("default namespace evicted");
        }
    }

    public DataStoreInfo getDefaultDataStore(
            WorkspaceInfo workspace, Callable<DataStoreInfo> loader) {
        return cache.get(generateDefaultDataStoreKey(workspace.getId()), loader);
    }

    public void evictDefaultDataStore(WorkspaceInfo ws) {
        evictDefaultDataStore(ws.getId(), ws.getName());
    }

    public void evictDefaultDataStore(String workspaceId, String name) {
        Object key = generateDefaultDataStoreKey(workspaceId);
        if (cache.evictIfPresent(key)) {
            log.trace("evicted default datastore for workspace {}", name);
        }
    }

    private class ResourceContainmentCache {

        private final Map<ReferencedInfo, Set<ReferencedInfo>> refsReverseIndex =
                new ConcurrentHashMap<>();

        public void put(CatalogInfo cached) {
            try {
                putInternal(cached);
            } catch (RuntimeException e) {
                log.warn("Error registering inverse refs cache for {}", cached, e);
            }
        }

        private void putInternal(CatalogInfo cached) {
            ReferencedInfo referrer = ReferencedInfo.valueOf(cached);
            cached.accept(new RefCollector(referrer, refsReverseIndex));
        }

        public void evict(String objectId, String prefixedName, ConfigInfoType type) {
            try {
                doEvict(objectId, prefixedName, type);
            } catch (RuntimeException e) {
                log.warn(
                        "Error evicting entries referring {}[id: {}, name: {}]",
                        type.type().getSimpleName(),
                        objectId,
                        prefixedName,
                        e);
            }
        }

        private void doEvict(String objectId, String prefixedName, ConfigInfoType type) {
            ReferencedInfo referee = new ReferencedInfo(objectId, prefixedName, type);
            Set<ReferencedInfo> referents = refsReverseIndex.remove(referee);
            if (null == referents) return;
            for (var ref : referents) {
                var referentId = ref.id();
                var referentPrefixedName = ref.prefixedName();
                var referentType = ref.type();
                log.debug("evicting {}, references evicted entry {}", ref, referee);
                boolean evicted =
                        CachingCatalogFacadeContainmentSupport.this.evictInternal(
                                referentId, referentPrefixedName, referentType, false);
                if (evicted) {
                    log.trace("evicted {}, referenced evicted entry {}", ref, referee);
                } else {
                    log.trace("{} already evicted or not present, referenced {}", ref, referee);
                }
            }
        }

        @RequiredArgsConstructor
        private static class RefCollector extends AbstractCatalogVisitor {
            final @NonNull ReferencedInfo referrer;
            final Map<ReferencedInfo, Set<ReferencedInfo>> reverseIndex;

            public @Override void visit(WorkspaceInfo ws) {
                add(ws);
            }

            public @Override void visit(NamespaceInfo ns) {
                add(ns);
            }

            public @Override void visit(StoreInfo store) {
                add(store);
                accept(store.getWorkspace());
            }

            public @Override void visit(ResourceInfo r) {
                add(r);
                accept(r.getNamespace());
                accept(r.getStore());
            }

            public @Override void visit(StyleInfo style) {
                add(style);
                accept(style.getWorkspace());
            }

            public @Override void visit(LayerInfo l) {
                add(l);
                accept(l.getResource());
                accept(l.getDefaultStyle());
                l.getStyles().forEach(this::accept);
            }

            public @Override void visit(LayerGroupInfo lg) {
                add(lg);
                accept(lg.getWorkspace());
                accept(lg.getRootLayer());
                accept(lg.getRootLayerStyle());
                lg.getLayers().forEach(this::accept);
                lg.getStyles().forEach(this::accept);
            }

            private void accept(@Nullable CatalogInfo ref) {
                if (ref != null) ref.accept(this);
            }

            private void add(CatalogInfo ref) {
                ReferencedInfo refInfo = ReferencedInfo.valueOf(ref);
                if (Objects.equals(this.referrer, refInfo)) return;

                var set = reverseIndex.computeIfAbsent(refInfo, oid -> new HashSet<>());
                synchronized (set) {
                    set.add(this.referrer);
                }
            }
        }

        private static record ReferencedInfo(String id, String prefixedName, ConfigInfoType type) {

            static ReferencedInfo valueOf(CatalogInfo info) {
                String infoId = info.getId();
                String prefixedName = InfoEvent.prefixedName(info);
                ConfigInfoType type = ConfigInfoType.valueOf(info);
                return new ReferencedInfo(infoId, prefixedName, type);
            }

            @Override
            public String toString() {
                return "%s[id: %s, name: %s]"
                        .formatted(type().type().getSimpleName(), id(), prefixedName());
            }
        }
    }

    public void evictAll() {
        cache.clear();
    }
}
