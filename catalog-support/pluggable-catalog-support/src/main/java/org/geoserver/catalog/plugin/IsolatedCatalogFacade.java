/*
 * (c) 2014 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans This
 * code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.geoserver.catalog.CatalogCapabilities;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.plugin.forwarding.ForwardingCatalogFacade;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.LocalWorkspace;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/** Copy of package private {@code org.geoserver.catalog.impl.IsolatedCatalogFacade} */
public final class IsolatedCatalogFacade extends ForwardingCatalogFacade{

    IsolatedCatalogFacade(CatalogFacade facade) {
        super(facade);
    }

    @Override
    public <T extends StoreInfo> T getStore(String id, Class<T> clazz) {
        return enforceStoreIsolation(facade.getStore(id, clazz));
    }

    @Override
    public <T extends StoreInfo> T getStoreByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {
        return canSeeWorkspace(workspace) ? facade.getStoreByName(workspace, name, clazz) : null;
    }

    @Override
    public <T extends StoreInfo> List<T> getStoresByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {
        return canSeeWorkspace(workspace)
                ? facade.getStoresByWorkspace(workspace, clazz)
                : Collections.emptyList();
    }

    @Override
    public <T extends StoreInfo> List<T> getStores(Class<T> clazz) {
        return filterIsolated(facade.getStores(clazz), clazz, this::enforceStoreIsolation);
    }

    @Override
    public DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace) {
        return enforceStoreIsolation(facade.getDefaultDataStore(workspace));
    }

    @Override
    public <T extends ResourceInfo> T getResource(String id, Class<T> clazz) {
        return enforceResourceIsolation(facade.getResource(id, clazz));
    }

    @Override
    public <T extends ResourceInfo> T getResourceByName(
            NamespaceInfo namespace, String name, Class<T> clazz) {
        NamespaceInfo localNamespace = tryMatchLocalNamespace(namespace);
        if (localNamespace != null) {
            // the URIs of the provided namespace and of the local workspace namespace matched
            return facade.getResourceByName(localNamespace, name, clazz);
        }
        return enforceResourceIsolation(facade.getResourceByName(namespace, name, clazz));
    }

    @Override
    public <T extends ResourceInfo> List<T> getResources(Class<T> clazz) {
        return filterIsolated(facade.getResources(clazz), clazz, this::enforceResourceIsolation);
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByNamespace(
            NamespaceInfo namespace, Class<T> clazz) {
        NamespaceInfo localNamespace = tryMatchLocalNamespace(namespace);
        if (localNamespace != null) {
            // the URIs of the provided namespace and of the local workspace namespace matched
            return facade.getResourcesByNamespace(localNamespace, clazz);
        }
        return filterIsolated(
                facade.getResourcesByNamespace(namespace, clazz),
                clazz,
                this::enforceResourceIsolation);
    }

    @Override
    public <T extends ResourceInfo> T getResourceByStore(
            StoreInfo store, String name, Class<T> clazz) {
        return enforceResourceIsolation(facade.getResourceByStore(store, name, clazz));
    }

    @Override
    public <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class<T> clazz) {
        return filterIsolated(
                facade.getResourcesByStore(store, clazz), clazz, this::enforceResourceIsolation);
    }

    @Override
    public LayerInfo getLayer(String id) {
        return enforceLayerIsolation(facade.getLayer(id));
    }

    @Override
    public LayerInfo getLayerByName(String name) {
        return enforceLayerIsolation(facade.getLayerByName(name));
    }

    @Override
    public List<LayerInfo> getLayers(ResourceInfo resource) {
        return filterIsolated(
                facade.getLayers(resource), LayerInfo.class, this::enforceLayerIsolation);
    }

    @Override
    public List<LayerInfo> getLayers(StyleInfo style) {
        return filterIsolated(
                facade.getLayers(style), LayerInfo.class, this::enforceLayerIsolation);
    }

    @Override
    public List<LayerInfo> getLayers() {
        return filterIsolated(facade.getLayers(), LayerInfo.class, this::enforceLayerIsolation);
    }

    @Override
    public LayerGroupInfo getLayerGroup(String id) {
        return enforceLayerGroupIsolation(facade.getLayerGroup(id));
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(String name) {
        return enforceLayerGroupIsolation(facade.getLayerGroupByName(name));
    }

    @Override
    public LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name) {
        return enforceLayerGroupIsolation(facade.getLayerGroupByName(workspace, name));
    }

    @Override
    public List<LayerGroupInfo> getLayerGroups() {
        return filterIsolated(
                facade.getLayerGroups(), LayerGroupInfo.class, this::enforceLayerGroupIsolation);
    }

    @Override
    public List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace) {
        return filterIsolated(
                facade.getLayerGroupsByWorkspace(workspace),
                LayerGroupInfo.class,
                this::enforceLayerGroupIsolation);
    }

    @Override
    public NamespaceInfo getNamespaceByURI(String uri) {
        NamespaceInfo localNamespace = getLocalNamespace();
        if (localNamespace != null && Objects.equals(localNamespace.getURI(), uri)) {
            // local workspace namespace URI is equal to the provided URI
            return localNamespace;
        }
        // let's see if there is any global namespace matching the provided uri
        for (NamespaceInfo namespace : facade.getNamespacesByURI(uri)) {
            if (!namespace.isIsolated()) {
                // we found a global namespace
                return namespace;
            }
        }
        // no global namespace found
        return null;
    }

    @Override
    public StyleInfo getStyle(String id) {
        return enforceStyleIsolation(facade.getStyle(id));
    }

    @Override
    public StyleInfo getStyleByName(String name) {
        return enforceStyleIsolation(facade.getStyleByName(name));
    }

    @Override
    public StyleInfo getStyleByName(WorkspaceInfo workspace, String name) {
        return enforceStyleIsolation(facade.getStyleByName(workspace, name));
    }

    @Override
    public List<StyleInfo> getStyles() {
        return filterIsolated(facade.getStyles(), StyleInfo.class, this::enforceStyleIsolation);
    }

    @Override
    public List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace) {
        return filterIsolated(
                facade.getStylesByWorkspace(workspace),
                StyleInfo.class,
                this::enforceStyleIsolation);
    }

    @Override
    public <T extends CatalogInfo> int count(Class<T> of, Filter filter) {
        CloseableIterator<T> found = facade.list(of, filter, null, null);

        try (CloseableIterator<T> filtered = filterIsolated(of, found)) {
            int count = 0;
            while (filtered.hasNext()) {
                count++;
                filtered.next();
            }
            return count;
        }
    }

    @Override
    public <T extends CatalogInfo> CloseableIterator<T> list(
            Class<T> of,
            Filter filter,
            @Nullable Integer offset,
            @Nullable Integer count,
            @Nullable SortBy... sortOrder) {
        return filterIsolated(of, facade.list(of, filter, offset, count, sortOrder));
    }

    @Override
    public CatalogCapabilities getCatalogCapabilities() {
        CatalogCapabilities capabilities = facade.getCatalogCapabilities();
        // this wrapper adds support for isolated workspaces
        capabilities.setIsolatedWorkspacesSupport(true);
        return capabilities;
    }

    /**
     * Helper method that just returns the current local workspace if available.
     *
     * @return current local workspace or NULL
     */
    private WorkspaceInfo getLocalWorkspace() {
        return LocalWorkspace.get();
    }

    /**
     * Checks if the provided store is visible in the current context.
     *
     * @param store the store to check, may be NULL
     * @return the store if visible, otherwise NULL
     */
    private <T extends StoreInfo> T enforceStoreIsolation(T store) {
        if (store == null) {
            // nothing to do, the store is already NULL
            return null;
        }
        WorkspaceInfo workspace = store.getWorkspace();
        // check if the store workspace is visible in this context
        return canSeeWorkspace(workspace) ? store : null;
    }

    /**
     * Checks if the provided resource is visible in the current context.
     *
     * @param resource the resource to check, may be NULL
     * @return the resource if visible, otherwise NULL
     */
    private <T extends ResourceInfo> T enforceResourceIsolation(T resource) {
        if (resource == null) {
            // nothing to do, the resource is already NULL
            return null;
        }
        // get the resource store
        StoreInfo store = resource.getStore();
        if (store == null) {
            // since we can't check if the store is visible we let it go
            return resource;
        }
        WorkspaceInfo workspace = store.getWorkspace();
        // check if the resource store workspace is visible in this context
        return canSeeWorkspace(workspace) ? resource : null;
    }

    /**
     * Checks if the provided layer is visible in the current context.
     *
     * @param layer the layer to check, may be NULL
     * @return the layer if visible, otherwise NULL
     */
    private <T extends LayerInfo> T enforceLayerIsolation(T layer) {
        if (layer == null) {
            // nothing to do, the layer is already NULL
            return null;
        }
        ResourceInfo resource = layer.getResource();
        if (resource == null) {
            // this should not happen, there is not much we can do
            return layer;
        }
        StoreInfo store = resource.getStore();
        if (store == null) {
            // since we can't check if the store is visible we let it go
            return layer;
        }
        WorkspaceInfo workspace = store.getWorkspace();
        // check if the layer resource store workspace is visible in this context
        return canSeeWorkspace(workspace) ? layer : null;
    }

    /**
     * Checks if the provided style is visible in the current context.
     *
     * @param style the style to check, may be NULL
     * @return the style if visible, otherwise NULL
     */
    private <T extends StyleInfo> T enforceStyleIsolation(T style) {
        if (style == null) {
            // nothing to do, the style is already NULL
            return null;
        }
        WorkspaceInfo workspace = style.getWorkspace();
        // check if the style workspace is visible in this context
        return canSeeWorkspace(workspace) ? style : null;
    }

    /**
     * Checks if the provided layer group is visible in the current context. Note that layer group
     * contained layer groups will not be filtered.
     *
     * @param layerGroup the layer group to check, may be NULL
     * @return the layer group if visible, otherwise NULL
     */
    private <T extends LayerGroupInfo> T enforceLayerGroupIsolation(T layerGroup) {
        if (layerGroup == null) {
            // nothing to do, the layer group is already NULL
            return null;
        }
        WorkspaceInfo workspace = layerGroup.getWorkspace();
        // check if the layer group workspace is visible in this context
        return canSeeWorkspace(workspace) ? layerGroup : null;
    }

    /**
     * Helper method that checks if the provided workspace is visible in the current context.
     *
     * <p>This method returns TRUE if the provided workspace is one of the default ones
     * (NO_WORKSPACE or ANY_WORKSPACE) or if the provided workspace is NULL or is not isolated. If
     * no OWS service request is in progress TRUE will also be returned.
     *
     * <p>If none of the conditions above is satisfied, then if a local workspace exists (i.e. we
     * are in the context of a virtual service) and if the local workspace matches the provided
     * workspace TRUE is returned, otherwise FALSE is returned.
     *
     * @param workspace the workspace to check for visibility
     * @return TRUE if the workspace is visible in the current context, otherwise FALSE
     */
    private boolean canSeeWorkspace(WorkspaceInfo workspace) {
        if (workspace == CatalogFacade.NO_WORKSPACE
                || workspace == CatalogFacade.ANY_WORKSPACE
                || workspace == null
                || !workspace.isIsolated()
                || Dispatcher.REQUEST.get() == null) {
            // the workspace content is visible in this context
            return true;
        }
        WorkspaceInfo localWorkspace = getLocalWorkspace();
        // the workspace content will be visible only if we are in the context of one
        // of its virtual services
        return localWorkspace != null
                && Objects.equals(localWorkspace.getName(), workspace.getName());
    }

    /**
     * Helper method that removes from a list the catalog objects not visible in the current
     * context. This method takes care of the proper modification proxy unwrapping \ wrapping.
     *
     * @param objects list of catalog object, wrapped with a modification proxy
     * @param type the class of the list objects
     * @param filter filter that checks if an element should be visible
     * @return a list wrapped with a modification proxy that contains the visible catalog objects
     */
    private <T extends CatalogInfo> List<T> filterIsolated(
            List<T> objects, Class<T> type, Function<T, T> filter) {
        // unwrap the catalog objects list
        List<T> unwrapped = ModificationProxy.unwrap(objects);
        // filter the non visible catalog objects and wrap the resulting list with a modification
        // proxy
        return ModificationProxy.createList(
                unwrapped
                        .stream()
                        .filter(store -> filter.apply(store) != null)
                        .collect(Collectors.toList()),
                type);
    }

    /**
     * Helper method that consumes a catalog objects iterator keeping only the ones visible in the
     * current context.
     *
     * @param objects iterator over catalog objects
     * @param filter filter that checks if an element should be visible
     * @return an iterator over the catalog objects visible in the current context
     */
    private <T extends CatalogInfo> CloseableIterator<T> filterIsolated(
            CloseableIterator<T> objects, Function<T, T> filter) {
        List<T> iterable = new ArrayList<>();
        // consume the iterator
        while (objects.hasNext()) {
            T object = objects.next();
            if (filter.apply(object) != null) {
                // this catalog object is visible in the current context
                iterable.add(object);
            }
        }
        // create an iterator for the visible catalog objects
        return new CloseableIteratorAdapter<>(iterable.iterator());
    }

    /**
     * If a local workspace is set (i.e. we are in the context of a virtual service) and if the URI
     * of the provided namespace matches the local workspace associate namespace URI, we return the
     * namespace associated with the current local workspace, otherwise NULL is returned.
     *
     * @param namespace the namespace we will try to match against the local workspace
     * @return the namespace associated with the local workspace if matched, otherwise NULL
     */
    private NamespaceInfo tryMatchLocalNamespace(NamespaceInfo namespace) {
        WorkspaceInfo localWorkspace = getLocalWorkspace();
        if (localWorkspace != null) {
            // get the namespace for the current local workspace
            NamespaceInfo localNamespace = facade.getNamespaceByPrefix(localWorkspace.getName());
            if (localNamespace != null
                    && Objects.equals(localNamespace.getURI(), namespace.getURI())) {
                // the URIs match, let's return the local workspace namespace
                return localNamespace;
            }
        }
        // the provided namespace doesn't match the local workspace namespace
        return null;
    }

    /**
     * If a local workspace is set returns the namespace associated to it.
     *
     * @return the namespace associated with the local workspace, or NULL if no local workspace is
     *     set
     */
    private NamespaceInfo getLocalNamespace() {
        WorkspaceInfo localWorkspace = getLocalWorkspace();
        if (localWorkspace != null) {
            // get the namespace associated with the local workspace
            return facade.getNamespaceByPrefix(localWorkspace.getName());
        }
        return null;
    }

    /**
     * Removes from a list of catalog objects the ones that are not visible in the current context.
     *
     * @param type type of the catalog objects that we cna iterate over
     * @param objects iterator over catalog objects
     * @return an iterator over the catalog objects visible in the current context
     */
    @SuppressWarnings("unchecked")
    private <T extends CatalogInfo> CloseableIterator<T> filterIsolated(
            Class<T> type, CloseableIterator<T> objects) {
        if (StoreInfo.class.isAssignableFrom(type)) {
            return (CloseableIterator<T>)
                    filterIsolated(
                            (CloseableIterator<StoreInfo>) objects, this::enforceStoreIsolation);
        } else if (ResourceInfo.class.isAssignableFrom(type)) {
            return (CloseableIterator<T>)
                    filterIsolated(
                            (CloseableIterator<ResourceInfo>) objects,
                            this::enforceResourceIsolation);
        } else if (LayerInfo.class.isAssignableFrom(type)) {
            return (CloseableIterator<T>)
                    filterIsolated(
                            (CloseableIterator<LayerInfo>) objects, this::enforceLayerIsolation);
        } else if (LayerGroupInfo.class.isAssignableFrom(type)) {
            return (CloseableIterator<T>)
                    filterIsolated(
                            (CloseableIterator<LayerGroupInfo>) objects,
                            this::enforceLayerGroupIsolation);
        } else if (StyleInfo.class.isAssignableFrom(type)) {
            return (CloseableIterator<T>)
                    filterIsolated(
                            (CloseableIterator<StyleInfo>) objects, this::enforceStyleIsolation);
        }
        // unknown type of catalog object, there is not much we can do so we just let it go
        return objects;
    }
}
