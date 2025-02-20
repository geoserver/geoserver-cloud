/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.base.Stopwatch;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.AbstractCatalogVisitor;
import org.springframework.lang.Nullable;

@RequiredArgsConstructor
@Slf4j(topic = "org.geoserver.cloud.catalog.cache")
class CachedReferenceCleaner {

    @NonNull
    private final Cache<?, ?> caffeine;

    static CachedReferenceCleaner newInstance(org.springframework.cache.@NonNull Cache cache) {
        Object nativeCache = cache.getNativeCache();
        if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineCache) {
            return new CachedReferenceCleaner(caffeineCache);
        }
        throw new UnsupportedOperationException("Expected Caffeine cache, got unsupported cache implementation: %s"
                .formatted(nativeCache.getClass().getCanonicalName()));
    }

    /**
     * Evicts ant {@link CatalogInfo} that has a reference to the evicted {@code InfoKey}, to avoid
     * having cached entries with references to stale objects that have been modified
     *
     * <p>For example, a when {@code InfoIdKey} targets a {@link WorkspaceInfo}, it'll also evict
     * all {@link StyleInfo}s, {@link LayerGroupInfo}s, etc that reference the workspace.
     *
     * @param evicted
     */
    public void cascadeEvict(InfoIdKey evicted) {
        var cache = caffeine.asMap();
        var initialSize = cache.size();
        var sw = Stopwatch.createStarted();
        final AtomicInteger visited = new AtomicInteger();
        int cascadedEvictCount;
        try {
            cascadedEvictCount = cascadeEvict(evicted, cache, visited);
        } catch (RuntimeException e) {
            log.warn("Error cascade-evicting cached entries referencing {}, clearing out the whole cache", evicted, e);
            caffeine.invalidateAll();
            return;
        }
        sw.stop();
        var finalSize = cache.size();
        if (cascadedEvictCount > 0 || visited.intValue() > 0)
            log.debug(
                    "cascade evicted {} entries referencing {} in {}. Size pre: {}, after: {}, visited: {}",
                    cascadedEvictCount,
                    evicted.id(),
                    sw,
                    initialSize,
                    finalSize,
                    visited);
    }

    @SuppressWarnings("java:S3864") // Stream.peek
    private int cascadeEvict(InfoIdKey idKey, ConcurrentMap<?, ?> cache, AtomicInteger visited) {

        CachedReferenceCleanerVisitor visitor = new CachedReferenceCleanerVisitor(cache, idKey);

        return cache.values().stream()
                .filter(CatalogInfo.class::isInstance)
                .map(CatalogInfo.class::cast)
                .filter(info -> this.canReference(info, idKey))
                .peek(i -> visited.incrementAndGet())
                .map(visitor::cascadeEvict)
                .reduce(0, (c1, c2) -> c1 + c2);
    }

    /**
     * @return whether {@code cached} can directly or indirectly reference {@code evicted}
     */
    private final boolean canReference(CatalogInfo cached, InfoIdKey evicted) {
        if (null == cached) return true;
        return switch (evicted.type()) {
            case WORKSPACE -> canReferenceWorkspace(cached, evicted);
            case NAMESPACE -> canReferenceNamespace(cached, evicted);
            case COVERAGESTORE, DATASTORE, WMSSTORE, WMTSSTORE -> canReferenceStore(cached, evicted);
            case COVERAGE, FEATURETYPE, WMSLAYER, WMTSLAYER -> canReferenceResource(cached, evicted);

                // evicted a LayerInfo, only LayerGroupInfos may reference it
            case LAYER -> cached instanceof LayerGroupInfo;

                // evicted a LayerGroupInfo, only other LayerGroupInfos may reference it
            case LAYERGROUP -> cached instanceof LayerGroupInfo;

            case STYLE -> canReferenceStyle(cached, evicted);
            default -> false;
        };
    }

    // evicted a StyleInfo, only PublishedInfos may reference it
    private boolean canReferenceStyle(CatalogInfo cached, InfoIdKey evicted) {
        return (cached instanceof StyleInfo s && s.getId().equals(evicted.id()))
                || (cached instanceof LayerInfo l
                        && (canReference(l.getDefaultStyle(), evicted)
                                || (!l.getStyles().isEmpty()
                                        && l.getStyles().stream().anyMatch(s -> canReference(s, evicted)))))
                || cached instanceof LayerGroupInfo;
    }

    // evicted a ResourceInfo, only PublishedInfos may be referencing it
    private boolean canReferenceResource(CatalogInfo cached, InfoIdKey evicted) {
        return (cached instanceof ResourceInfo r && r.getId().equals(evicted.id()))
                || (cached instanceof LayerInfo l && canReference(l.getResource(), evicted))
                || cached instanceof LayerGroupInfo;
    }

    // evicted a StoreInfo, any ResourceInfo or PublishedInfo can be referencing it
    private boolean canReferenceStore(CatalogInfo cached, InfoIdKey evicted) {
        return (cached instanceof StoreInfo s && s.getId().equals(evicted.id()))
                || (cached instanceof ResourceInfo r && canReference(r.getStore(), evicted))
                || (cached instanceof LayerInfo l && canReference(l.getResource(), evicted))
                || cached instanceof LayerGroupInfo;
    }

    // evicted a namespace, only ResourceInfo, or PublishedInfo (indirectly) may
    // reference it
    private boolean canReferenceNamespace(CatalogInfo cached, InfoIdKey evicted) {
        return (cached instanceof NamespaceInfo ns && ns.getId().equals(evicted.id()))
                || (cached instanceof ResourceInfo r && canReference(r.getNamespace(), evicted))
                || (cached instanceof LayerInfo l && canReference(l.getResource(), evicted))
                || cached instanceof LayerGroupInfo;
    }

    // evicted a workspace, any object but a workspace or namespace reference it
    private boolean canReferenceWorkspace(CatalogInfo cached, InfoIdKey evicted) {
        return (cached instanceof WorkspaceInfo w && w.getId().equals(evicted.id()))
                || (cached instanceof StyleInfo s && canReference(s.getWorkspace(), evicted))
                || (cached instanceof LayerInfo l && canReference(l.getResource(), evicted))
                || (cached instanceof ResourceInfo r && canReference(r.getStore(), evicted))
                || (cached instanceof StoreInfo s && canReference(s.getWorkspace(), evicted))
                || (cached instanceof LayerGroupInfo lg
                        && (lg.getWorkspace() == null || canReference(lg.getWorkspace(), evicted)));
    }

    @RequiredArgsConstructor
    private static class CachedReferenceCleanerVisitor extends AbstractCatalogVisitor {

        @NonNull
        private final ConcurrentMap<?, ?> cache;

        /** key for the evicted object. Will evict any cached object that has a reference to it */
        @NonNull
        private final InfoIdKey evictedKey;

        /**
         * The cached object being traversed, to be evicted if it has any nested reference to {@link
         * #evictedKey}
         */
        private CatalogInfo cached;

        /**
         * Number of cascaded evictions (0, 1 or 2 for {@link #cached}'s InfoIdKey and/or
         * InfoNameKey)
         */
        @Getter
        private int count;

        public int cascadeEvict(CatalogInfo cached) {
            this.cached = cached;
            this.count = 0;
            cached.accept(this);
            return getCount();
        }

        private boolean accept(@Nullable CatalogInfo ref) {
            if (null != ref && evictedKey.id().equals(ref.getId())) {
                InfoIdKey idKey = InfoIdKey.valueOf(cached);
                InfoNameKey nameKey = InfoNameKey.valueOf(cached);
                evict(idKey);
                evict(nameKey);
            }
            return count > 0;
        }

        private void evict(Object key) {
            Object removed = cache.remove(key);
            if (null != removed) {
                ++count;
                log.trace("cascade evicted {} referencing {}", key, evictedKey.id());
            }
        }

        private void traverse(@Nullable CatalogInfo info) {
            if (null != info) info.accept(this);
        }

        public @Override void visit(WorkspaceInfo ws) {
            accept(ws);
        }

        public @Override void visit(NamespaceInfo ns) {
            accept(ns);
        }

        public @Override void visit(StoreInfo store) {
            accept(store);
            if (0 == count) traverse(store.getWorkspace());
        }

        public @Override void visit(ResourceInfo r) {
            accept(r);
            if (0 == count) traverse(r.getNamespace());
            if (0 == count) traverse(r.getStore());
        }

        public @Override void visit(StyleInfo style) {
            accept(style);
            if (0 == count) traverse(style.getWorkspace());
        }

        public @Override void visit(LayerInfo l) {
            accept(l);
            if (0 == count) traverse(l.getResource());
            if (0 == count) traverse(l.getDefaultStyle());
            if (0 == count) l.getStyles().forEach(this::traverse);
        }

        public @Override void visit(LayerGroupInfo lg) {
            accept(lg);
            if (0 == count) traverse(lg.getWorkspace());
            if (0 == count) traverse(lg.getRootLayer());
            if (0 == count) traverse(lg.getRootLayerStyle());
            if (0 == count) lg.getLayers().forEach(this::traverse);
            if (0 == count) lg.getStyles().forEach(this::traverse);
            if (0 == count)
                lg.getLayerGroupStyles().forEach(lgs -> {
                    lgs.getStyles().forEach(this::traverse);
                    lgs.getLayers().forEach(this::traverse);
                });
        }
    }
}
