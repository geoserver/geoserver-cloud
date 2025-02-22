/*
 * (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import java.rmi.server.UID;
import java.util.LinkedHashSet;
import java.util.List;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.impl.ResourceInfoImpl;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.plugin.CatalogInfoLookup.LayerGroupInfoLookup;
import org.geoserver.catalog.plugin.CatalogInfoLookup.LayerInfoLookup;
import org.geoserver.catalog.plugin.CatalogInfoLookup.MapInfoLookup;
import org.geoserver.catalog.plugin.CatalogInfoLookup.NamespaceInfoLookup;
import org.geoserver.catalog.plugin.CatalogInfoLookup.ResourceInfoLookup;
import org.geoserver.catalog.plugin.CatalogInfoLookup.StoreInfoLookup;
import org.geoserver.catalog.plugin.CatalogInfoLookup.StyleInfoLookup;
import org.geoserver.catalog.plugin.CatalogInfoLookup.WorkspaceInfoLookup;
import org.geoserver.ows.util.OwsUtils;

/**
 * A default in-memory implementation of {@link CatalogFacade} that uses {@link CatalogInfoLookup}
 * instances to store {@link CatalogInfo} objects.
 *
 * <p>This class extends {@link RepositoryCatalogFacadeImpl} to provide a memory-based catalog facade
 * for GeoServer Cloud, leveraging in-memory {@link CatalogInfoLookup} repositories for all core catalog
 * info types (e.g., workspaces, layers, styles). It manages catalog data in memory, resolving references
 * between objects (e.g., layers to resources, stores to workspaces) during initialization or updates.
 * This implementation is lightweight and suitable for testing or small-scale deployments where persistence
 * to an external store is not required.
 *
 * <p>Key features:
 * <ul>
 *   <li><strong>In-Memory Storage:</strong> Uses lookup-based repositories (e.g., {@link LayerInfoLookup})
 *       for fast, transient storage of catalog data.</li>
 *   <li><strong>Reference Resolution:</strong> Resolves proxies and ensures object relationships (e.g.,
 *       layer-to-resource links) via {@link #resolve()} and type-specific resolve methods.</li>
 *   <li><strong>Automatic ID Generation:</strong> Assigns unique IDs to objects lacking them using
 *       {@link #setId(Object)}.</li>
 * </ul>
 *
 * <p>The facade initializes with default in-memory repositories and supports all standard catalog
 * operations (e.g., add, remove, query) inherited from {@link RepositoryCatalogFacadeImpl}, with added
 * logic for in-memory reference management.
 *
 * @since 1.0
 * @see RepositoryCatalogFacadeImpl
 * @see CatalogInfoLookup
 * @see CatalogFacade
 */
@Slf4j
public class DefaultMemoryCatalogFacade extends RepositoryCatalogFacadeImpl implements CatalogFacade {

    /**
     * Constructs a new in-memory catalog facade with no associated catalog.
     *
     * <p>Initializes the facade with default in-memory repositories for all catalog info types. Use
     * {@link #setCatalog(Catalog)} to associate a catalog instance after construction if needed.
     */
    public DefaultMemoryCatalogFacade() {
        this(null);
    }

    /**
     * Constructs a new in-memory catalog facade with the specified catalog.
     *
     * <p>Initializes the facade with default in-memory repositories (e.g., {@link WorkspaceInfoLookup})
     * and associates it with the provided catalog. The repositories are configured to handle all core
     * {@link CatalogInfo} types, with the {@link ResourceInfoLookup} linked to the layer repository for
     * consistency.
     *
     * @param catalog The {@link Catalog} instance to associate with this facade; may be null.
     */
    public DefaultMemoryCatalogFacade(Catalog catalog) {
        super(catalog);
        setNamespaceRepository(new NamespaceInfoLookup());
        setWorkspaceRepository(new WorkspaceInfoLookup());
        setStoreRepository(new StoreInfoLookup());
        setLayerRepository(new LayerInfoLookup());
        setResourceRepository(new ResourceInfoLookup((LayerInfoLookup) getLayerRepository()));
        setLayerGroupRepository(new LayerGroupInfoLookup());
        setMapRepository(new MapInfoLookup());
        setStyleRepository(new StyleInfoLookup());
    }

    /**
     * Resolves references for all catalog objects in memory.
     *
     * <p>Iterates through all repositories (workspaces, namespaces, stores, etc.) and resolves internal
     * references (e.g., layer-to-resource, store-to-workspace) by calling type-specific resolve methods.
     * Ensures that all {@link CatalogInfo} objects are fully resolved and consistent, replacing proxies
     * with actual instances where possible.
     *
     * @example Resolving catalog state:
     *          <pre>
     *          DefaultMemoryCatalogFacade facade = new DefaultMemoryCatalogFacade(catalog);
     *          facade.resolve();
     *          </pre>
     */
    @Override
    public void resolve() {
        getWorkspaceRepository().findAll().forEach(this::resolve);
        getNamespaceRepository().findAll().forEach(this::resolve);
        getStoreRepository().findAll().forEach(this::resolve);
        getStyleRepository().findAll().forEach(this::resolve);
        getResourceRepository().findAll().forEach(this::resolve);
        getLayerRepository().findAll().forEach(this::resolve);
        getLayerGroupRepository().findAll().forEach(this::resolve);
        getMapRepository().findAll().forEach(this::resolve);
    }

    /**
     * Resolves references within a {@link LayerInfo} object.
     *
     * <p>Ensures the layer has an ID (assigning one if missing), resolves its resource and default style
     * to actual instances from the catalog, and updates its additional styles set by replacing proxies
     * with resolved objects.
     *
     * @param layer The {@link LayerInfo} to resolve; must not be null.
     * @throws NullPointerException if {@code layer} is null.
     */
    protected void resolve(LayerInfo layer) {
        setId(layer);

        ResourceInfo resource = ResolvingProxy.resolve(getCatalog(), layer.getResource());
        if (resource != null) {
            resource = unwrap(resource);
            layer.setResource(resource);
        }

        StyleInfo style = ResolvingProxy.resolve(getCatalog(), layer.getDefaultStyle());
        if (style != null) {
            style = unwrap(style);
            layer.setDefaultStyle(style);
        }

        LinkedHashSet<StyleInfo> styles = new LinkedHashSet<>();
        for (StyleInfo s : layer.getStyles()) {
            s = ResolvingProxy.resolve(getCatalog(), s);
            s = unwrap(s);
            styles.add(s);
        }
        ((LayerInfoImpl) layer).setStyles(styles);
    }

    /**
     * Resolves references within a {@link LayerGroupInfo} object.
     *
     * <p>Ensures the layer group has an ID (assigning one if missing), resolves its layers and styles lists,
     * and recursively resolves nested layers and styles within any {@link LayerGroupStyle} objects.
     *
     * @param layerGroup The {@link LayerGroupInfo} to resolve; must not be null.
     * @throws NullPointerException if {@code layerGroup} is null.
     */
    protected void resolve(LayerGroupInfo layerGroup) {
        setId(layerGroup);

        LayerGroupInfoImpl lg = (LayerGroupInfoImpl) layerGroup;

        resolveLayerGroupLayers(lg.getLayers());
        resolveLayerGroupStyles(lg.getLayers(), lg.getStyles());
        // now resolves layers and styles defined in layer group styles
        for (LayerGroupStyle groupStyle : lg.getLayerGroupStyles()) {
            resolveLayerGroupLayers(groupStyle.getLayers());
            resolveLayerGroupStyles(groupStyle.getLayers(), groupStyle.getStyles());
        }
    }

    /**
     * Resolves styles within a layer group, handling special cases for layer group style references.
     *
     * <p>Iterates through the styles list, resolving each style to its catalog instance. If a style
     * represents a {@link LayerGroupStyle} reference (not stored in the catalog), creates a new
     * {@link StyleInfo} instance based on its name. Ensures the styles align with their assigned layers.
     *
     * @param assignedLayers The list of {@link PublishedInfo} objects (layers or groups) corresponding to
     *                       the styles; must not be null.
     * @param styles         The list of {@link StyleInfo} objects to resolve; must not be null.
     * @throws NullPointerException if {@code assignedLayers} or {@code styles} is null.
     */
    private void resolveLayerGroupStyles(List<PublishedInfo> assignedLayers, List<StyleInfo> styles) {
        for (int i = 0; i < styles.size(); i++) {
            StyleInfo s = styles.get(i);
            if (s != null) {
                PublishedInfo assignedLayer = assignedLayers.get(i);
                StyleInfo resolved = null;
                if (assignedLayer instanceof LayerGroupInfo) {
                    // special case we might have a StyleInfo representing
                    // only the name of a LayerGroupStyle thus not present in Catalog.
                    // We take the ref and create a new object
                    // without searching in catalog.
                    String ref = ResolvingProxy.getRef(s);
                    if (ref != null) {
                        StyleInfo styleInfo = new StyleInfoImpl(getCatalog());
                        styleInfo.setName(ref);
                        resolved = styleInfo;
                    }
                }
                if (resolved == null) resolved = unwrap(ResolvingProxy.resolve(getCatalog(), s));

                styles.set(i, resolved);
            }
        }
    }

    /**
     * Resolves layers within a layer group’s layers list.
     *
     * <p>Iterates through the layers list, resolving each {@link PublishedInfo} (layer or nested group)
     * to its catalog instance and updating the list with resolved objects.
     *
     * @param layers The list of {@link PublishedInfo} objects to resolve; must not be null.
     * @throws NullPointerException if {@code layers} is null.
     */
    private void resolveLayerGroupLayers(List<PublishedInfo> layers) {
        for (int i = 0; i < layers.size(); i++) {
            PublishedInfo published = layers.get(i);

            if (published != null) {
                PublishedInfo resolved = resolveLayerGroupLayers(published);
                layers.set(i, resolved);
            }
        }
    }

    /**
     * Resolves a single {@link PublishedInfo} object (layer or layer group) within a layer group.
     *
     * <p>Resolves the object to its catalog instance, falling back to the original object if resolution
     * fails (e.g., during catalog loading when nested objects aren’t yet available).
     *
     * @param published The {@link PublishedInfo} to resolve (either a {@link LayerInfo} or
     *                  {@link LayerGroupInfo}); must not be null.
     * @return The resolved {@link PublishedInfo}, or the original if unresolved.
     * @throws NullPointerException if {@code published} is null.
     */
    private PublishedInfo resolveLayerGroupLayers(@NonNull PublishedInfo published) {
        PublishedInfo resolved = unwrap(ResolvingProxy.resolve(getCatalog(), published));
        // special case to handle catalog loading, when nested publishables might not be loaded.
        if (resolved == null && (published instanceof LayerInfo || published instanceof LayerGroupInfo)) {
            resolved = published;
        }
        return resolved;
    }

    /**
     * Resolves references within a {@link StyleInfo} object.
     *
     * <p>Ensures the style has an ID (assigning one if missing) and resolves its workspace to the actual
     * catalog instance, logging a message if resolution fails (e.g., workspace not yet added).
     *
     * @param style The {@link StyleInfo} to resolve; must not be null.
     * @throws NullPointerException if {@code style} is null.
     */
    protected void resolve(StyleInfo style) {
        setId(style);

        // resolve the workspace
        WorkspaceInfo ws = style.getWorkspace();
        if (ws != null) {
            WorkspaceInfo resolved = ResolvingProxy.resolve(getCatalog(), ws);
            if (resolved != null) {
                resolved = unwrap(resolved);
                style.setWorkspace(resolved);
            } else {
                log.info(
                        """
                        Failed to resolve workspace for style {}.
                        This means the workspace has not yet been added to the catalog, keep the proxy around
                        """,
                        style.getName());
            }
        }
    }

    /**
     * Resolves references within a {@link MapInfo} object.
     *
     * <p>Ensures the map has an ID (assigning one if missing). No additional references are resolved as
     * maps typically lack complex dependencies in this implementation.
     *
     * @param map The {@link MapInfo} to resolve; must not be null.
     * @throws NullPointerException if {@code map} is null.
     */
    protected void resolve(MapInfo map) {
        setId(map);
    }

    /**
     * Resolves references within a {@link WorkspaceInfo} object.
     *
     * <p>Ensures the workspace has an ID (assigning one if missing). No additional references are resolved
     * as workspaces are top-level objects in this implementation.
     *
     * @param workspace The {@link WorkspaceInfo} to resolve; must not be null.
     * @throws NullPointerException if {@code workspace} is null.
     */
    protected void resolve(WorkspaceInfo workspace) {
        setId(workspace);
    }

    /**
     * Resolves references within a {@link NamespaceInfo} object.
     *
     * <p>Ensures the namespace has an ID (assigning one if missing). No additional references are resolved
     * as namespaces are top-level objects in this implementation.
     *
     * @param namespace The {@link NamespaceInfo} to resolve; must not be null.
     * @throws NullPointerException if {@code namespace} is null.
     */
    protected void resolve(NamespaceInfo namespace) {
        setId(namespace);
    }

    /**
     * Resolves references within a {@link StoreInfo} object.
     *
     * <p>Ensures the store has an ID (assigning one if missing) and resolves its workspace to the actual
     * catalog instance, logging a message if resolution fails (e.g., workspace not yet added).
     *
     * @param store The {@link StoreInfo} to resolve; must not be null.
     * @throws NullPointerException if {@code store} is null.
     */
    protected void resolve(StoreInfo store) {
        setId(store);
        StoreInfoImpl s = (StoreInfoImpl) store;

        // resolve the workspace
        WorkspaceInfo resolved = ResolvingProxy.resolve(getCatalog(), s.getWorkspace());
        if (resolved != null) {
            resolved = unwrap(resolved);
            s.setWorkspace(resolved);
        } else {
            log.info(
                    """
                    Failed to resolve workspace for store {}.
                    This means the workspace has not yet been added to the catalog, keep the proxy around
                    """,
                    store.getName());
        }
    }

    /**
     * Resolves references within a {@link ResourceInfo} object.
     *
     * <p>Ensures the resource has an ID (assigning one if missing), resolves its store and namespace to
     * actual catalog instances, and updates the resource accordingly.
     *
     * @param resource The {@link ResourceInfo} to resolve; must not be null.
     * @throws NullPointerException if {@code resource} is null.
     */
    protected void resolve(ResourceInfo resource) {
        setId(resource);
        ResourceInfoImpl r = (ResourceInfoImpl) resource;

        // resolve the store
        StoreInfo store = ResolvingProxy.resolve(getCatalog(), r.getStore());
        if (store != null) {
            store = unwrap(store);
            r.setStore(store);
        }

        // resolve the namespace
        NamespaceInfo namespace = ResolvingProxy.resolve(getCatalog(), r.getNamespace());
        if (namespace != null) {
            namespace = unwrap(namespace);
            r.setNamespace(namespace);
        }
    }

    /**
     * Assigns a unique ID to a catalog object if it lacks one.
     *
     * <p>Generates a unique ID using {@link UID} prefixed with the object’s class name (e.g.,
     * "WorkspaceInfo-uid") and sets it via {@link OwsUtils#set(Object, String, Object)}.
     *
     * @param o The object to assign an ID to; must not be null.
     * @throws NullPointerException if {@code o} is null.
     */
    protected void setId(Object o) {
        if (OwsUtils.get(o, "id") == null) {
            String uid = new UID().toString();
            String id = "%s-%s".formatted(o.getClass().getSimpleName(), uid);
            OwsUtils.set(o, "id", id);
        }
    }

    /**
     * Unwraps a {@link CatalogInfo} object from its proxy, if applicable.
     *
     * <p>Delegates to {@link ModificationProxy#unwrap(Object)} to remove any proxy wrapper, returning the
     * underlying instance or null if the input is null.
     *
     * @param <T> The type of {@link CatalogInfo}.
     * @param obj The object to unwrap; may be null.
     * @return The unwrapped {@link CatalogInfo}, or null if {@code obj} is null.
     */
    @Nullable
    public static <T> T unwrap(@Nullable T obj) {
        return ModificationProxy.unwrap(obj);
    }
}
