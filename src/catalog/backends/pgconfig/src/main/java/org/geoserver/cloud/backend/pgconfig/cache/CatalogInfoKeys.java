/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.cache;

import static org.geoserver.cloud.event.info.ConfigInfoType.COVERAGE;
import static org.geoserver.cloud.event.info.ConfigInfoType.COVERAGESTORE;
import static org.geoserver.cloud.event.info.ConfigInfoType.DATASTORE;
import static org.geoserver.cloud.event.info.ConfigInfoType.FEATURETYPE;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYER;
import static org.geoserver.cloud.event.info.ConfigInfoType.LAYERGROUP;
import static org.geoserver.cloud.event.info.ConfigInfoType.NAMESPACE;
import static org.geoserver.cloud.event.info.ConfigInfoType.STYLE;
import static org.geoserver.cloud.event.info.ConfigInfoType.WMSLAYER;
import static org.geoserver.cloud.event.info.ConfigInfoType.WMSSTORE;
import static org.geoserver.cloud.event.info.ConfigInfoType.WMTSLAYER;
import static org.geoserver.cloud.event.info.ConfigInfoType.WMTSSTORE;
import static org.geoserver.cloud.event.info.ConfigInfoType.WORKSPACE;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey.Kind;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.jspecify.annotations.Nullable;

/**
 * The keys of catalog objects in the {@link EntityCache}.
 *
 * <p>An object has an {@code ENTITY} key under its id, and a {@code NAME} key under its prefixed name, as computed by
 * {@link InfoEvent#prefixedName}. A layer also has a {@code NAME} key under its local name: a lookup of a layer by name
 * matches the prefixed name or the local name, and keeps its miss under the name looked up. A group or style without
 * workspace has no {@code NAME} key if its name holds a colon, see {@link #globalName(String)}, and the lookups of such
 * a name without workspace keep their misses under a key of their own, see {@link #globalNameMiss(ConfigInfoType,
 * String)}. A layer or group also has a {@code TILE_LAYER} key under its id, for its GWC settings. The settings are in
 * the row of the object: its lookups read them, and the evictions of its keys include them.
 *
 * <p>Prefixed names derive from the names of other objects, such as the workspace of a store. A change of such a name
 * makes the names of other objects stale, see {@link #dependentKeys(ConfigInfoType, Patch)}.
 */
public final class CatalogInfoKeys {

    /** The prefix of the keys of {@link #namespaceByUri(String)}. */
    private static final String URI_PREFIX = "uri:";

    /** The first character of the keys of {@link #globalNameMiss(ConfigInfoType, String)}. */
    private static final char MISS_MARK = '\u0000';

    private static final Set<ConfigInfoType> STORE_TYPES = EnumSet.of(DATASTORE, COVERAGESTORE, WMSSTORE, WMTSSTORE);

    private static final Set<ConfigInfoType> RESOURCE_TYPES = EnumSet.of(FEATURETYPE, COVERAGE, WMSLAYER, WMTSLAYER);

    /** The types named after the name of a workspace. */
    private static final Set<ConfigInfoType> WORKSPACE_DEPENDENTS =
            EnumSet.of(DATASTORE, COVERAGESTORE, WMSSTORE, WMTSSTORE, STYLE, LAYERGROUP);

    /** The types named after the prefix of a namespace. */
    private static final Set<ConfigInfoType> NAMESPACE_DEPENDENTS =
            EnumSet.of(FEATURETYPE, COVERAGE, WMSLAYER, WMTSLAYER, LAYER);

    private CatalogInfoKeys() {
        // static methods only
    }

    /**
     * Returns the keys of an object: its {@code ENTITY} key, its {@code NAME} keys, for a namespace the key of
     * {@link #namespaceByUri(String)}, and for a layer or group the key of its GWC settings. The {@code NAME} keys are
     * left out if the object holds a reference not resolved yet, as its name cannot be computed. A group or style
     * without workspace has the key of {@link #globalNameMiss(ConfigInfoType, String)} instead of a {@code NAME} key if
     * its name holds a colon.
     */
    public static List<CacheKey> keysOf(CatalogInfo info) {
        ConfigInfoType type = ConfigInfoType.valueOf(info);
        List<CacheKey> keys = entityAndNameKeys(type, info.getId(), prefixedName(info));
        Optional<CacheKey> missKey = globalNameMissOf(type, nameWithoutWorkspace(info));
        missKey.ifPresent(keys::add);
        if (info instanceof NamespaceInfo namespace && null != namespace.getURI()) {
            keys.add(namespaceByUri(namespace.getURI()));
        }
        Optional<CacheKey> tileLayerKey = tileLayerOf(type, info.getId());
        tileLayerKey.ifPresent(keys::add);
        return keys;
    }

    /**
     * Returns the {@code ENTITY} key and the {@code NAME} keys of an object, from its id and its name as an event tells
     * them, and for a layer or group the key of its GWC settings. The name {@code ws:lg} of a group or style may be the
     * prefixed name of {@code lg} in {@code ws}, or the name of an object without workspace: such a name also gives the
     * key of {@link #globalNameMiss(ConfigInfoType, String)}.
     */
    public static List<CacheKey> keysOf(ConfigInfoType type, String id, @Nullable String prefixedName) {
        List<CacheKey> keys = entityAndNameKeys(type, id, prefixedName);
        Optional<CacheKey> missKey = globalNameMissOf(type, prefixedName);
        missKey.ifPresent(keys::add);
        Optional<CacheKey> tileLayerKey = tileLayerOf(type, id);
        tileLayerKey.ifPresent(keys::add);
        return keys;
    }

    private static List<CacheKey> entityAndNameKeys(ConfigInfoType type, String id, @Nullable String prefixedName) {
        List<CacheKey> keys = new ArrayList<>();
        keys.add(CacheKey.entity(type, id));
        if (null != prefixedName) {
            keys.addAll(nameKeys(type, prefixedName));
        }
        return keys;
    }

    /**
     * The key of {@link #globalNameMiss(ConfigInfoType, String)} for the name of a group or style if the name holds a
     * colon. Empty otherwise.
     */
    private static Optional<CacheKey> globalNameMissOf(ConfigInfoType type, @Nullable String name) {
        boolean groupOrStyle = type == LAYERGROUP || type == STYLE;
        if (groupOrStyle && null != name && InfoEvent.prefix(name).isPresent()) {
            return Optional.of(globalNameMiss(type, name));
        }
        return Optional.empty();
    }

    /**
     * The key of the GWC settings of a layer or group, see {@link CacheKey#tileLayer(String)}. Empty for other types.
     */
    private static Optional<CacheKey> tileLayerOf(ConfigInfoType type, String id) {
        if (type == LAYER || type == LAYERGROUP) {
            return Optional.of(CacheKey.tileLayer(id));
        }
        return Optional.empty();
    }

    /** The name of a group or style without workspace, null for another object. */
    private static @Nullable String nameWithoutWorkspace(CatalogInfo info) {
        return switch (info) {
            case LayerGroupInfo group when null == group.getWorkspace() -> group.getName();
            case StyleInfo style when null == style.getWorkspace() -> style.getName();
            default -> null;
        };
    }

    /** Returns the {@code NAME} keys of a prefixed name. A layer also has a key under its local name. */
    public static List<CacheKey> nameKeys(ConfigInfoType type, String prefixedName) {
        CacheKey name = CacheKey.name(type, prefixedName);
        String localName = InfoEvent.localName(prefixedName);
        if (type != LAYER || localName.equals(prefixedName)) {
            return List.of(name);
        }
        return List.of(name, CacheKey.name(LAYER, localName));
    }

    /** The key holding the id of the default workspace. */
    public static CacheKey defaultWorkspace() {
        return CacheKey.lookup(WORKSPACE, "default");
    }

    /** The key holding the id of the default namespace. */
    public static CacheKey defaultNamespace() {
        return CacheKey.lookup(NAMESPACE, "default");
    }

    /** The key holding the id of the default data store of a workspace. */
    public static CacheKey defaultDataStore(String workspaceId) {
        return CacheKey.lookup(DATASTORE, "default:" + workspaceId);
    }

    /** The key holding the id of the first namespace with a URI, by id. */
    public static CacheKey namespaceByUri(String uri) {
        return CacheKey.lookup(NAMESPACE, URI_PREFIX + uri);
    }

    /**
     * Returns the keys of other objects made stale by a change, and empty if the change makes none stale.
     *
     * <p>A rename of a workspace changes the names of its stores, styles and groups. A rename of a namespace changes
     * the names of its resources and layers, and a rename of a resource the name of its layer. These changes make the
     * {@code NAME} and {@code LOOKUP} keys of the named types stale: a key may hold an old name, or a miss for a new
     * name.
     *
     * <p>A store moved to another workspace makes its resources stale too: the database moves them to the namespace of
     * the new workspace.
     */
    public static Optional<Predicate<CacheKey>> dependentKeys(ConfigInfoType type, Patch patch) {
        if (type == WORKSPACE && changes(patch, "name")) {
            return Optional.of(namesAndLookupsOf(WORKSPACE_DEPENDENTS));
        }
        if (type == NAMESPACE && (changes(patch, "prefix") || changes(patch, "name"))) {
            return Optional.of(namesAndLookupsOf(NAMESPACE_DEPENDENTS));
        }
        if (RESOURCE_TYPES.contains(type) && (changes(patch, "name") || changes(patch, "namespace"))) {
            return Optional.of(namesAndLookupsOf(EnumSet.of(LAYER)));
        }
        if (STORE_TYPES.contains(type) && changes(patch, "workspace")) {
            Predicate<CacheKey> resources = key -> RESOURCE_TYPES.contains(key.type());
            return Optional.of(resources.or(namesAndLookupsOf(EnumSet.of(LAYER))));
        }
        return Optional.empty();
    }

    /**
     * Matches the {@code NAME} keys of a type with a local name, whatever their prefix. A move to another workspace or
     * namespace changes the prefix of a name, and these keys hold the old name among them.
     */
    public static Predicate<CacheKey> namesWithLocalName(ConfigInfoType type, String localName) {
        Predicate<CacheKey> names = namesOf(type);
        return names.and(key -> localName.equals(InfoEvent.localName(key.key())));
    }

    /** Matches the {@code NAME} keys of a type. */
    public static Predicate<CacheKey> namesOf(ConfigInfoType type) {
        return key -> key.type() == type && key.kind() == Kind.NAME;
    }

    /** Matches the {@code LOOKUP} keys of a type, of catalog or configuration objects. */
    public static Predicate<CacheKey> lookupsOf(ConfigInfoType type) {
        return key -> key.type() == type && key.kind() == Kind.LOOKUP;
    }

    /** Matches the keys of {@link #namespaceByUri(String)}, and not the key of the default namespace. */
    public static Predicate<CacheKey> namespaceUriLookups() {
        Predicate<CacheKey> lookups = lookupsOf(NAMESPACE);
        return lookups.and(key -> key.key().startsWith(URI_PREFIX));
    }

    /**
     * The prefixed name of an object in a workspace, or null if the workspace is a reference not resolved yet. An
     * object without workspace has the name given by {@link #globalName(String)}.
     */
    public static @Nullable String prefixedName(@Nullable WorkspaceInfo workspace, @Nullable String name) {
        if (null == workspace) {
            return globalName(name);
        }
        return joined(workspace.getName(), name);
    }

    /**
     * Returns the name of the {@code NAME} key of a group or style without workspace: its own name. Null if the name
     * holds a colon: the object then has no {@code NAME} key, and the lookups of its name without workspace keep their
     * misses under the key of {@link #globalNameMiss(ConfigInfoType, String)}.
     *
     * <p>A name like {@code ws:lg} is also the prefixed name of the group {@code lg} of the workspace {@code ws}. The
     * catalog looks it up without workspace first, then in the workspace of the prefix. With one key for both lookups,
     * the miss of the first lookup would answer the second.
     */
    public static @Nullable String globalName(@Nullable String name) {
        if (null == name || InfoEvent.prefix(name).isPresent()) {
            return null;
        }
        return name;
    }

    /**
     * Returns the key remembering that a lookup without workspace found no group or style with the name. The name holds
     * a colon, such as {@code ws:lg}: the catalog runs this lookup each time it looks up the group {@code lg} of
     * {@code ws} by its prefixed name.
     *
     * <p>The key is a {@code NAME} key. Its name starts with a character rejected by PostgreSQL in text and jsonb,
     * followed by the name looked up: no stored name equals it, and the rows put no id under it. A lookup finding the
     * object puts its id under {@code LOOKUP} keys only, and the key holds misses only.
     */
    public static CacheKey globalNameMiss(ConfigInfoType type, String name) {
        return CacheKey.name(type, MISS_MARK + name);
    }

    /** The prefixed name of an object in a namespace, or null if the namespace is a reference not resolved yet. */
    public static @Nullable String prefixedName(@Nullable NamespaceInfo namespace, @Nullable String name) {
        if (null == namespace) {
            return name;
        }
        return joined(namespace.getPrefix(), name);
    }

    /**
     * Whether an object has a prefixed name, as far as the object tells without resolving its references. A workspace
     * or a namespace must have the whole name. Another object must have the local name. The name has a prefix if the
     * object belongs to a workspace or a namespace, and none otherwise. A layer takes the name of its resource.
     *
     * <p>The prefix itself is not compared: an object read from the cache holds its workspace or namespace as a
     * reference not resolved yet, and the getters of the reference answer null. A rename of a workspace or namespace
     * evicts the keys holding its old name, see {@link #dependentKeys(ConfigInfoType, Patch)}.
     */
    public static boolean hasName(CatalogInfo info, String prefixedName) {
        return switch (info) {
            case WorkspaceInfo workspace -> prefixedName.equals(workspace.getName());
            case NamespaceInfo namespace -> prefixedName.equals(namespace.getPrefix());
            case StoreInfo store -> hasLocalName(prefixedName, store.getWorkspace(), store.getName());
            case ResourceInfo resource -> hasLocalName(prefixedName, resource.getNamespace(), resource.getName());
            case LayerInfo layer -> hasResourceName(layer, prefixedName);
            case LayerGroupInfo group -> hasLocalName(prefixedName, group.getWorkspace(), group.getName());
            case StyleInfo style -> hasLocalName(prefixedName, style.getWorkspace(), style.getName());
            default -> false;
        };
    }

    private static boolean hasResourceName(LayerInfo layer, String prefixedName) {
        ResourceInfo resource = layer.getResource();
        return null != resource && hasName(resource, prefixedName);
    }

    /**
     * Whether a name is the name of an object, with a prefix if the object has a workspace or namespace. Without one,
     * the object has the name given by {@link #globalName(String)}: none if its name holds a colon.
     */
    private static boolean hasLocalName(String prefixedName, @Nullable CatalogInfo prefix, @Nullable String name) {
        if (null == prefix) {
            return prefixedName.equals(globalName(name));
        }
        boolean prefixed = InfoEvent.prefix(prefixedName).isPresent();
        return prefixed && InfoEvent.localName(prefixedName).equals(name);
    }

    private static boolean changes(Patch patch, String property) {
        return patch.get(property).isPresent();
    }

    private static Predicate<CacheKey> namesAndLookupsOf(Set<ConfigInfoType> types) {
        Predicate<CacheKey> namesAndLookups = key -> key.kind() == Kind.NAME || key.kind() == Kind.LOOKUP;
        return namesAndLookups.and(key -> types.contains(key.type()));
    }

    /**
     * The prefixed name of an object, as {@link InfoEvent#prefixedName} computes it. Null if it is unknown, and if the
     * object has no {@code NAME} key.
     */
    private static @Nullable String prefixedName(CatalogInfo info) {
        return switch (info) {
            case WorkspaceInfo workspace -> workspace.getName();
            case NamespaceInfo namespace -> namespace.getPrefix();
            case StoreInfo store -> prefixedName(store.getWorkspace(), store.getName());
            case ResourceInfo resource -> prefixedName(resource.getNamespace(), resource.getName());
            case LayerInfo layer -> layerName(layer);
            case LayerGroupInfo group -> prefixedName(group.getWorkspace(), group.getName());
            case StyleInfo style -> prefixedName(style.getWorkspace(), style.getName());
            default -> null;
        };
    }

    /** A layer takes the prefixed name of its resource. */
    private static @Nullable String layerName(LayerInfo layer) {
        ResourceInfo resource = layer.getResource();
        if (null == resource) {
            return null;
        }
        return prefixedName(resource.getNamespace(), resource.getName());
    }

    // The getters of a reference not resolved yet answer null, except for its id
    private static @Nullable String joined(@Nullable String prefix, @Nullable String name) {
        if (null == prefix || null == name) {
            return null;
        }
        return InfoEvent.prefixedName(Optional.of(prefix), name);
    }
}
