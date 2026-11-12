/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.IsolatedCatalogFacade;
import org.jspecify.annotations.Nullable;

/**
 * Finds the objects referenced by {@link ResolvingProxy} instances in an {@link ExtendedCatalogFacade}, with the rules
 * of {@code ResolvingProxy.resolve(Catalog, Object)}: a reference holds the id of an object, or its name when no object
 * has that id.
 *
 * <p>A facade placed below {@link IsolatedCatalogFacade} resolves the references of its objects with this class. The
 * catalog cannot do it, because its lookups go through workspace isolation. In a request outside an isolated workspace,
 * the store of a layer of that workspace would not be found, and the layer would pass the isolation check with an
 * unresolved store.
 *
 * <p>The objects come back as the facade returns them: their own references may still be proxies.
 */
public final class FacadeReferenceLookup {

    private final ExtendedCatalogFacade facade;

    public FacadeReferenceLookup(@NonNull ExtendedCatalogFacade facade) {
        this.facade = facade;
    }

    /**
     * Returns the object referenced by a proxy, or null if the facade does not hold it. An object other than a proxy
     * comes back unchanged, and a proxy of a map too: upstream does not resolve maps either.
     *
     * <p>A name prefixed by a workspace or namespace, such as {@code topp:states}, is looked up in that workspace or
     * namespace, and a bare name in any of them.
     */
    @SuppressWarnings("unchecked")
    public <T extends Info> @Nullable T resolve(T proxy) {
        ResolvingProxy handler = resolvingProxyOf(proxy);
        if (handler == null) {
            return proxy;
        }
        String ref = handler.getRef();
        if (ref == null || ref.isEmpty()) {
            // a placeholder for no object, such as the missing layer of a style group
            return null;
        }
        return (T) find(proxy, ref, handler.getPrefix());
    }

    /** Finds the objects of the ids with one call to the facade, keyed by id. */
    <T extends CatalogInfo> Map<String, T> findAllById(List<String> ids, Class<T> type) {
        try (Stream<T> found = facade.findAllById(ids, type)) {
            return found.collect(Collectors.toMap(CatalogInfo::getId, Function.identity()));
        }
    }

    private @Nullable Info find(Info proxy, String ref, @Nullable String prefix) {
        return switch (proxy) {
            case WorkspaceInfo _ -> workspace(ref);
            case NamespaceInfo _ -> namespace(ref);
            case StoreInfo _ -> store(ref, referencedType(proxy, StoreInfo.class));
            case ResourceInfo _ -> resource(ref, referencedType(proxy, ResourceInfo.class));
            case LayerInfo _ -> layer(ref);
            case LayerGroupInfo _ -> layerGroup(ref);
            case PublishedInfo _ -> facade.getPublished(ref);
            case StyleInfo _ -> style(ref, prefix);
            default -> proxy;
        };
    }

    private @Nullable WorkspaceInfo workspace(String ref) {
        WorkspaceInfo workspace = facade.getWorkspace(ref);
        if (workspace == null) {
            workspace = facade.getWorkspaceByName(ref);
        }
        return workspace;
    }

    private @Nullable NamespaceInfo namespace(String ref) {
        NamespaceInfo namespace = facade.getNamespace(ref);
        if (namespace == null) {
            namespace = facade.getNamespaceByPrefix(ref);
        }
        return namespace;
    }

    private @Nullable StoreInfo store(String ref, Class<? extends StoreInfo> type) {
        StoreInfo store = facade.getStore(ref, type);
        if (store == null) {
            store = storeByName(PrefixedName.of(ref), type);
        }
        return store;
    }

    private @Nullable StoreInfo storeByName(PrefixedName name, Class<? extends StoreInfo> type) {
        if (name.prefix() == null) {
            return facade.getStoreByName(CatalogFacade.ANY_WORKSPACE, name.localName(), type);
        }
        WorkspaceInfo workspace = facade.getWorkspaceByName(name.prefix());
        if (workspace == null) {
            return null;
        }
        return facade.getStoreByName(workspace, name.localName(), type);
    }

    private @Nullable ResourceInfo resource(String ref, Class<? extends ResourceInfo> type) {
        ResourceInfo resource = facade.getResource(ref, type);
        if (resource == null) {
            resource = resourceByName(PrefixedName.of(ref), type);
        }
        return resource;
    }

    private @Nullable ResourceInfo resourceByName(PrefixedName name, Class<? extends ResourceInfo> type) {
        if (name.prefix() == null) {
            return facade.getResourceByName(CatalogFacade.ANY_NAMESPACE, name.localName(), type);
        }
        NamespaceInfo namespace = facade.getNamespaceByPrefix(name.prefix());
        if (namespace == null) {
            return null;
        }
        return facade.getResourceByName(namespace, name.localName(), type);
    }

    private @Nullable LayerInfo layer(String ref) {
        LayerInfo layer = facade.getLayer(ref);
        if (layer == null) {
            layer = facade.getLayerByName(ref);
        }
        return layer;
    }

    private @Nullable LayerGroupInfo layerGroup(String ref) {
        LayerGroupInfo layerGroup = facade.getLayerGroup(ref);
        if (layerGroup == null) {
            layerGroup = layerGroupByName(PrefixedName.of(ref));
        }
        return layerGroup;
    }

    /** A bare name is the name of a layer group without workspace. */
    private @Nullable LayerGroupInfo layerGroupByName(PrefixedName name) {
        if (name.prefix() == null) {
            return facade.getLayerGroupByName(name.localName());
        }
        WorkspaceInfo workspace = facade.getWorkspaceByName(name.prefix());
        if (workspace == null) {
            return null;
        }
        return facade.getLayerGroupByName(workspace, name.localName());
    }

    /** Looks a style up by id, then by name in the workspace named by the prefix of the proxy, then by name. */
    private @Nullable StyleInfo style(String ref, @Nullable String workspaceName) {
        StyleInfo style = facade.getStyle(ref);
        if (style == null && workspaceName != null) {
            style = styleInWorkspace(workspaceName, ref);
        }
        if (style == null) {
            style = facade.getStyleByName(ref);
        }
        return style;
    }

    private @Nullable StyleInfo styleInWorkspace(String workspaceName, String name) {
        WorkspaceInfo workspace = facade.getWorkspaceByName(workspaceName);
        if (workspace == null) {
            return null;
        }
        return facade.getStyleByName(workspace, name);
    }

    /**
     * The interface given to {@link ResolvingProxy#create(String, Class)}, such as {@code DataStoreInfo} for the store
     * of a feature type. The lookup asks the facade for that type, as upstream does for data stores and feature types.
     */
    private static <B extends Info> Class<? extends B> referencedType(Info proxy, Class<B> base) {
        for (Class<?> type : proxy.getClass().getInterfaces()) {
            if (base.isAssignableFrom(type)) {
                return type.asSubclass(base);
            }
        }
        return base;
    }

    private static @Nullable ResolvingProxy resolvingProxyOf(@Nullable Info info) {
        if (info != null
                && Proxy.isProxyClass(info.getClass())
                && Proxy.getInvocationHandler(info) instanceof ResolvingProxy handler) {
            return handler;
        }
        return null;
    }

    /** A name such as {@code topp:states}, split at its first colon. The prefix of a bare name is null. */
    private record PrefixedName(@Nullable String prefix, String localName) {

        static PrefixedName of(String name) {
            int colon = name.indexOf(':');
            if (colon > 0) {
                return new PrefixedName(name.substring(0, colon), name.substring(colon + 1));
            }
            return new PrefixedName(null, name);
        }
    }
}
