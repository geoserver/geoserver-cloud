/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.requestcache;

import static org.geoserver.catalog.impl.ClassMappings.LAYER;
import static org.geoserver.catalog.impl.ClassMappings.LAYERGROUP;
import static org.geoserver.catalog.impl.ClassMappings.NAMESPACE;
import static org.geoserver.catalog.impl.ClassMappings.PUBLISHED;
import static org.geoserver.catalog.impl.ClassMappings.RESOURCE;
import static org.geoserver.catalog.impl.ClassMappings.STORE;
import static org.geoserver.catalog.impl.ClassMappings.STYLE;
import static org.geoserver.catalog.impl.ClassMappings.WORKSPACE;
import static org.geoserver.catalog.plugin.requestcache.RequestCacheAccess.evict;
import static org.geoserver.catalog.plugin.requestcache.RequestCacheAccess.liveCache;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.CatalogInfoTypeRegistry;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.RepositoryCatalogFacadeImpl;
import org.geoserver.catalog.plugin.forwarding.ForwardingExtendedCatalogFacade;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;
import org.jspecify.annotations.Nullable;

/**
 * Remembers the catalog lookups of a request: by id, by name, and of the defaults. GeoServer looks the same objects up
 * many times within a request, and the facade below would decode an object at each lookup.
 *
 * <p>The answers go to the {@link RequestCache} bound to the thread by {@link RequestCacheScope}, the lookups finding
 * nothing included. The later lookups of the request receive the same instances. Outside a scope, and once the request
 * ended, the lookups run uncached. The listings and the counts are never cached. A request remembers the objects it
 * looked up, and the references looked up to resolve the objects of a listing, but not the listed objects: a listing
 * reads most of its objects once, and would fill the cache of the request.
 *
 * <p>The request sees its own writes. An addition evicts the misses cached for the id and the name of the new object. A
 * change or a removal clears the cache of the request: the cached objects may hold the changed object, and the names of
 * other objects may derive from its name. The configuration lookups of {@link RequestCachingGeoServerFacade} share the
 * cache, and a catalog change clears them too. Writes are rare on the read paths, and a request writing a change
 * usually ends after it. A change made by another thread of this instance shows from the next request on. A change made
 * through another service instance shows at the next lookup, see {@link RequestCache}.
 */
public class RequestCachingCatalogFacade extends ForwardingExtendedCatalogFacade {

    private static final LookupKey DEFAULT_WORKSPACE = new LookupKey("default-workspace");

    private static final LookupKey DEFAULT_NAMESPACE = new LookupKey("default-namespace");

    /** The prefix of the keys of the default data stores, followed by the id of the workspace. */
    private static final String DEFAULT_DATASTORE = "default-datastore:";

    public RequestCachingCatalogFacade(@NonNull ExtendedCatalogFacade facade) {
        super(facade);
    }

    @Override
    public WorkspaceInfo getWorkspace(String id) {
        return memoized(idKey(WORKSPACE, id), () -> super.getWorkspace(id));
    }

    @Override
    public NamespaceInfo getNamespace(String id) {
        return memoized(idKey(NAMESPACE, id), () -> super.getNamespace(id));
    }

    /** Caches under the type asked, such as {@code STORE} for {@code StoreInfo}. */
    @Override
    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        IdKey key = idKey(typeOfLookup(clazz), id);
        return memoized(key, () -> super.getStore(id, clazz));
    }

    /** Caches under the type asked, such as {@code RESOURCE} for {@code ResourceInfo}. */
    @Override
    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        IdKey key = idKey(typeOfLookup(clazz), id);
        return memoized(key, () -> super.getResource(id, clazz));
    }

    @Override
    public LayerInfo getLayer(String id) {
        return memoized(idKey(LAYER, id), () -> super.getLayer(id));
    }

    @Override
    public LayerGroupInfo getLayerGroup(String id) {
        return memoized(idKey(LAYERGROUP, id), () -> super.getLayerGroup(id));
    }

    @Override
    public StyleInfo getStyle(String id) {
        return memoized(idKey(STYLE, id), () -> super.getStyle(id));
    }

    /** Answers from the key of the layer, then of the group, then of the lookups by {@code PublishedInfo}. */
    @Override
    public PublishedInfo getPublished(@NonNull String id) {
        return memoized(idKey(PUBLISHED, id), () -> super.getPublished(id));
    }

    /**
     * Answers a lookup from the cache of the request, or runs it and caches its answer, as
     * {@link RequestCacheAccess#memoized(Object, Supplier)} does. A lookup by id may be answered from the key of
     * another type, see {@link #cachedAnswer(RequestCache, Object)} and {@link #remember(RequestCache, Object,
     * Object)}.
     */
    private static <T extends CatalogInfo> @Nullable T memoized(@Nullable Object key, Supplier<@Nullable T> lookup) {
        return RequestCacheAccess.memoized(
                key, lookup, RequestCachingCatalogFacade::cachedAnswer, RequestCachingCatalogFacade::remember);
    }

    /**
     * Returns what the cache holds for a lookup: empty if the lookup is not cached, {@link RequestCache#MISS} if it
     * found nothing, the object found otherwise.
     *
     * <p>A lookup by id of a {@code PublishedInfo} also finds a layer or a group cached under its own key. A miss under
     * the key of the lookup means the id is neither a layer nor a group.
     */
    private static Optional<Object> cachedAnswer(RequestCache cache, Object key) {
        if (key instanceof IdKey(ClassMappings type, String id) && type == PUBLISHED) {
            return cachedPublished(cache, id);
        }
        return cache.found(key);
    }

    private static Optional<Object> cachedPublished(RequestCache cache, String id) {
        Optional<Object> layer = cachedObject(cache, new IdKey(LAYER, id));
        Optional<Object> layerOrGroup = layer.or(() -> cachedObject(cache, new IdKey(LAYERGROUP, id)));
        return layerOrGroup.or(() -> cache.found(new IdKey(PUBLISHED, id)));
    }

    /** The object cached under a key, empty if the key is not cached or holds a miss. */
    private static Optional<Object> cachedObject(RequestCache cache, Object key) {
        Optional<Object> cached = cache.found(key);
        return cached.filter(value -> value != RequestCache.MISS);
    }

    /**
     * Caches the answer of a lookup, a miss if it found nothing. An object found by id also goes under its own key: a
     * data store found by {@code StoreInfo} then answers the lookups by {@code DataStoreInfo}.
     */
    private static void remember(RequestCache cache, Object key, @Nullable Object found) {
        cache.put(key, found);
        if (key instanceof IdKey && found instanceof CatalogInfo info) {
            IdKey ownKey = idKeyOf(info);
            if (!ownKey.equals(key)) {
                cache.put(ownKey, info);
            }
        }
    }

    /** The key of a lookup by id, or null without type or id: the lookup then runs uncached. */
    private static @Nullable IdKey idKey(@Nullable ClassMappings type, @Nullable String id) {
        if (type == null || id == null) {
            return null;
        }
        return new IdKey(type, id);
    }

    /**
     * The type of the lookups by a class, such as {@code STORE} for {@code StoreInfo}. Null for a class without a
     * {@link ClassMappings} constant, such as {@code CatalogInfo}.
     */
    private static @Nullable ClassMappings typeOfLookup(@Nullable Class<? extends CatalogInfo> clazz) {
        if (clazz == null) {
            return null;
        }
        return ClassMappings.fromInterface(clazz);
    }

    @Override
    public WorkspaceInfo getWorkspaceByName(String name) {
        return memoized(nameKey(WORKSPACE, name), () -> super.getWorkspaceByName(name));
    }

    @Override
    public NamespaceInfo getNamespaceByPrefix(String prefix) {
        return memoized(nameKey(NAMESPACE, prefix), () -> super.getNamespaceByPrefix(prefix));
    }

    /** Caches a lookup in a workspace under the prefixed name, such as {@code topp:roads}. */
    @Override
    public <T extends StoreInfo> T getStoreByName(WorkspaceInfo workspace, String name, Class<T> clazz) {
        NameKey key = prefixedKey(typeOfLookup(clazz), workspaceName(workspace), name);
        return memoized(key, () -> super.getStoreByName(workspace, name, clazz));
    }

    /** Caches a lookup in a namespace under the prefixed name, such as {@code topp:roads}. */
    @Override
    public <T extends ResourceInfo> T getResourceByName(NamespaceInfo namespace, String name, Class<T> clazz) {
        NameKey key = prefixedKey(typeOfLookup(clazz), namespacePrefix(namespace), name);
        return memoized(key, () -> super.getResourceByName(namespace, name, clazz));
    }

    /** Caches under the name as given, with or without the prefix of a namespace. */
    @Override
    public LayerInfo getLayerByName(String name) {
        return memoized(nameKey(LAYER, name), () -> super.getLayerByName(name));
    }

    /**
     * Looks a group without workspace up, like the lookup with {@code NO_WORKSPACE}, and shares its key. The facade
     * below must give both lookups the same answer, as {@link RepositoryCatalogFacadeImpl} does. A
     * {@link ResolvingCatalogFacadeDecorator} in between passes the name on unchanged, and keeps that answer.
     */
    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {
        return memoized(globalKey(LAYERGROUP, name), () -> super.getLayerGroupByName(name));
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        NameKey key = keyInWorkspace(LAYERGROUP, workspace, name);
        return memoized(key, () -> super.getLayerGroupByName(workspace, name));
    }

    /**
     * Looks a style up without workspace, then in any workspace. Only a style without workspace, or a miss, goes under
     * the key of the global style: the style of a workspace would answer the later lookups of the global style. A miss
     * under that key does not answer this lookup, as a style of a workspace may still have the name.
     */
    @Override
    public StyleInfo getStyleByName(String name) {
        RequestCache cache = liveCache();
        NameKey key = globalKey(STYLE, name);
        if (cache == null || key == null) {
            return super.getStyleByName(name);
        }
        Optional<Object> cached = cachedObject(cache, key);
        if (cached.isPresent()) {
            return (StyleInfo) cached.get();
        }
        StyleInfo found = super.getStyleByName(name);
        if (answersGlobalLookup(found)) {
            cache.put(key, found);
        }
        return found;
    }

    /** Whether the answer of a lookup by bare name is also the answer of the lookup of the global style. */
    private static boolean answersGlobalLookup(@Nullable StyleInfo style) {
        return style == null || style.getWorkspace() == null;
    }

    @Override
    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        NameKey key = keyInWorkspace(STYLE, workspace, name);
        return memoized(key, () -> super.getStyleByName(workspace, name));
    }

    /** The key of a lookup by a name without prefix, or null without name. */
    private static @Nullable NameKey nameKey(ClassMappings type, @Nullable String name) {
        if (name == null) {
            return null;
        }
        return new NameKey(type, name);
    }

    /**
     * The key of a lookup by name in a workspace or namespace: the name after the prefix, as in {@code topp:roads}.
     * Null without type, prefix or name.
     */
    private static @Nullable NameKey prefixedKey(
            @Nullable ClassMappings type, @Nullable String prefix, @Nullable String name) {
        if (type == null || prefix == null || name == null) {
            return null;
        }
        return new NameKey(type, prefix + ":" + name);
    }

    /** The key of a lookup of a group or style in a workspace, or without workspace for {@code NO_WORKSPACE}. */
    private static @Nullable NameKey keyInWorkspace(
            ClassMappings type, @Nullable WorkspaceInfo workspace, @Nullable String name) {
        if (workspace == NO_WORKSPACE) {
            return globalKey(type, name);
        }
        return prefixedKey(type, workspaceName(workspace), name);
    }

    /**
     * The key of a lookup of a group or style without workspace: the bare name. A name with a colon gets no key. The
     * catalog looks a prefixed name up without workspace first, then in the workspace of the prefix. Both lookups would
     * have the same key, and the miss of the first one would answer the second one.
     */
    private static @Nullable NameKey globalKey(ClassMappings type, @Nullable String name) {
        if (name == null || name.contains(":")) {
            return null;
        }
        return new NameKey(type, name);
    }

    /** The name of a workspace as the prefix of a lookup, or null for any workspace or none. */
    private static @Nullable String workspaceName(@Nullable WorkspaceInfo workspace) {
        if (workspace == null || workspace == ANY_WORKSPACE || workspace == NO_WORKSPACE) {
            return null;
        }
        return workspace.getName();
    }

    /** The prefix of a namespace as the prefix of a lookup, or null for any namespace. */
    private static @Nullable String namespacePrefix(@Nullable NamespaceInfo namespace) {
        if (namespace == null || namespace == ANY_NAMESPACE) {
            return null;
        }
        return namespace.getPrefix();
    }

    @Override
    public WorkspaceInfo getDefaultWorkspace() {
        return memoized(DEFAULT_WORKSPACE, super::getDefaultWorkspace);
    }

    @Override
    public void setDefaultWorkspace(WorkspaceInfo workspace) {
        super.setDefaultWorkspace(workspace);
        evict(DEFAULT_WORKSPACE);
    }

    @Override
    public NamespaceInfo getDefaultNamespace() {
        return memoized(DEFAULT_NAMESPACE, super::getDefaultNamespace);
    }

    @Override
    public void setDefaultNamespace(NamespaceInfo defaultNamespace) {
        super.setDefaultNamespace(defaultNamespace);
        evict(DEFAULT_NAMESPACE);
    }

    @Override
    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return memoized(defaultDataStoreKey(workspace), () -> super.getDefaultDataStore(workspace));
    }

    @Override
    public void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store) {
        super.setDefaultDataStore(workspace, store);
        evict(defaultDataStoreKey(workspace));
    }

    /** The key of the default data store of a workspace, or null without workspace id. */
    private static @Nullable LookupKey defaultDataStoreKey(@Nullable WorkspaceInfo workspace) {
        if (workspace == null || workspace.getId() == null) {
            return null;
        }
        return new LookupKey(DEFAULT_DATASTORE + workspace.getId());
    }

    /**
     * Answers the ids cached by the request, and fetches the other ids with one call below. A cached miss leaves its id
     * out. An id absent from the answer below is cached as a miss.
     */
    @Override
    public <T extends CatalogInfo> Stream<T> findAllById(@NonNull List<String> ids, @NonNull Class<T> type) {
        RequestCache cache = liveCache();
        ClassMappings keyType = typeOfLookup(type);
        if (cache == null || keyType == null) {
            return super.findAllById(ids, type);
        }
        CachedIds<T> cached = cachedIds(cache, keyType, ids, type);
        List<T> fetched = fetchById(cache, keyType, cached.uncached(), type);
        List<T> found = new ArrayList<>(cached.found());
        found.addAll(fetched);
        return found.stream();
    }

    /** The answers of the cache to a lookup of several ids: the objects found, and the ids not cached. */
    private record CachedIds<T>(List<T> found, List<String> uncached) {}

    private static <T extends CatalogInfo> CachedIds<T> cachedIds(
            RequestCache cache, ClassMappings keyType, List<String> ids, Class<T> type) {
        List<T> found = new ArrayList<>();
        List<String> uncached = new ArrayList<>();
        for (String id : new LinkedHashSet<>(ids)) {
            Optional<Object> cached = cachedAnswer(cache, new IdKey(keyType, id));
            if (cached.isEmpty()) {
                uncached.add(id);
            } else if (cached.get() != RequestCache.MISS) {
                found.add(type.cast(cached.get()));
            }
        }
        return new CachedIds<>(found, uncached);
    }

    /**
     * Fetches the objects of the ids with one call below, caches each object as the call gives it, then caches a miss
     * for the other ids. The facade below may resolve an object before it gives the next one, and the lookups made
     * meanwhile may cache an object of a later id. The request keeps the instance cached first, and the answer gives it
     * in place of the one fetched.
     */
    private <T extends CatalogInfo> List<T> fetchById(
            RequestCache cache, ClassMappings keyType, List<String> ids, Class<T> type) {
        if (ids.isEmpty()) {
            return List.of();
        }
        List<T> fetched;
        try (Stream<T> stream = super.findAllById(ids, type)) {
            fetched = stream.map(info -> rememberFetched(cache, keyType, info, type))
                    .toList();
        }
        rememberMisses(cache, keyType, ids, fetched);
        return fetched;
    }

    /**
     * Caches an object fetched by id, and returns the instance kept by the request: the instance cached under the key
     * of the object if there is one, the object fetched otherwise.
     */
    private static <T extends CatalogInfo> T rememberFetched(
            RequestCache cache, ClassMappings keyType, T fetched, Class<T> type) {
        Object held = cache.held(idKeyOf(fetched));
        T kept = fetched;
        if (held instanceof CatalogInfo cached) {
            kept = type.cast(cached);
        }
        remember(cache, new IdKey(keyType, fetched.getId()), kept);
        return kept;
    }

    /** Caches a miss for the ids absent from the objects fetched. */
    private static void rememberMisses(
            RequestCache cache, ClassMappings keyType, List<String> ids, List<? extends CatalogInfo> fetched) {
        Set<String> fetchedIds = new HashSet<>();
        for (CatalogInfo info : fetched) {
            fetchedIds.add(info.getId());
        }
        for (String id : ids) {
            if (!fetchedIds.contains(id)) {
                remember(cache, new IdKey(keyType, id), null);
            }
        }
    }

    /** Evicts the misses cached for the id and the name of the new object. */
    @Override
    public <T extends CatalogInfo> T add(@NonNull T info) {
        T added = super.add(info);
        evictKeysOf(info);
        return added;
    }

    /**
     * Clears the cache of the request once the change is written. The cached objects may hold the changed object: the
     * cached layers of a renamed style hold the style with its old name.
     */
    @Override
    public <I extends CatalogInfo> I update(I info, Patch patch) {
        I updated = super.update(info, patch);
        clearRequest();
        return updated;
    }

    /** Clears the cache of the request once the object is removed, as {@link #update(CatalogInfo, Patch)} does. */
    @Override
    public void remove(@NonNull CatalogInfo info) {
        super.remove(info);
        clearRequest();
    }

    /**
     * Clears the cache of the request, the configuration lookups of {@link RequestCachingGeoServerFacade} included:
     * both facades share the cache.
     */
    private static void clearRequest() {
        RequestCache cache = liveCache();
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Evicts the keys of a new object: a lookup may have cached a miss under its id or its name. The name has no
     * prefix, and matches the name keys with any prefix or none: the name of the workspace or namespace of the object
     * may be unknown here.
     */
    private static void evictKeysOf(CatalogInfo info) {
        RequestCache cache = liveCache();
        if (cache != null) {
            ClassMappings type = typeOf(info);
            Predicate<Object> keys = keysOf(type, info.getId(), localNameOf(info));
            cache.evictIf(keys);
        }
    }

    /** Matches the keys of an object: its id keys, and the name keys holding its name, bare or after a prefix. */
    private static Predicate<Object> keysOf(ClassMappings type, @Nullable String id, @Nullable String name) {
        Set<ClassMappings> idTypes = idTypesOf(type);
        Set<ClassMappings> nameTypes = nameTypesOf(type);
        Predicate<String> named = namedAs(name);
        return key -> switch (key) {
            case IdKey idKey -> idTypes.contains(idKey.type()) && Objects.equals(id, idKey.id());
            case NameKey nameKey -> nameTypes.contains(nameKey.type()) && named.test(nameKey.name());
            default -> false;
        };
    }

    /**
     * The types of the id keys of an object: its own type, and the abstract type of the lookups by {@code StoreInfo},
     * {@code ResourceInfo} or {@code PublishedInfo}.
     */
    private static Set<ClassMappings> idTypesOf(ClassMappings type) {
        return switch (type) {
            case DATASTORE, COVERAGESTORE, WMSSTORE, WMTSSTORE -> EnumSet.of(type, STORE);
            case FEATURETYPE, COVERAGE, WMSLAYER, WMTSLAYER -> EnumSet.of(type, RESOURCE);
            case LAYER, LAYERGROUP -> EnumSet.of(type, PUBLISHED);
            default -> EnumSet.of(type);
        };
    }

    /** The types of the name keys of an object: the types of its id keys, and the layers for a resource. */
    private static Set<ClassMappings> nameTypesOf(ClassMappings type) {
        Set<ClassMappings> types = idTypesOf(type);
        if (types.contains(RESOURCE)) {
            // a layer has the name of its resource
            types.add(LAYER);
        }
        return types;
    }

    /**
     * Matches the names of name keys: the name, bare or after a prefix such as {@code topp:}. An unknown name matches
     * any name. The predicate runs on each key of the request: the ending of the prefixed names is built once.
     */
    private static Predicate<String> namedAs(@Nullable String name) {
        if (name == null) {
            return keyName -> true;
        }
        String prefixedEnding = ":" + name;
        return keyName -> keyName.equals(name) || keyName.endsWith(prefixedEnding);
    }

    /** The type of an object, such as {@code DATASTORE}: the constant of the interface of its class. */
    private static ClassMappings typeOf(CatalogInfo info) {
        Class<CatalogInfo> type = CatalogInfoTypeRegistry.resolveType(info);
        return ClassMappings.fromInterface(type);
    }

    private static IdKey idKeyOf(CatalogInfo info) {
        return new IdKey(typeOf(info), info.getId());
    }

    /**
     * The name of an object without prefix: the prefix of a namespace, the name of its resource for a layer. Null if
     * unknown, such as for a layer with a resource not resolved yet.
     */
    private static @Nullable String localNameOf(CatalogInfo info) {
        return switch (info) {
            case WorkspaceInfo workspace -> workspace.getName();
            case NamespaceInfo namespace -> namespace.getPrefix();
            case StoreInfo store -> store.getName();
            case ResourceInfo resource -> resource.getName();
            case PublishedInfo published -> published.getName();
            case StyleInfo style -> style.getName();
            case MapInfo map -> map.getName();
            default -> null;
        };
    }
}
