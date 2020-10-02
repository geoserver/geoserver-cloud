/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ResolvingProxy;

/**
 * {@link ResolvingCatalogFacade#setObjectResolver resolving function} that resolves {@link
 * CatalogInfo} properties that are proxied through {@link ResolvingProxy} before returning the
 * object from the facade.
 *
 * @see ResolvingProxy
 */
public class ResolvingProxyResolver implements Function<CatalogInfo, CatalogInfo> {

    private final Catalog catalog;
    private final BiConsumer<CatalogInfo, ResolvingProxy> onNotFound;

    public ResolvingProxyResolver(Catalog catalog) {
        this(catalog, (info, proxy) -> {});
    }

    public ResolvingProxyResolver(
            Catalog catalog, BiConsumer<CatalogInfo, ResolvingProxy> onNotFound) {
        requireNonNull(catalog);
        requireNonNull(onNotFound);
        this.catalog = catalog;
        this.onNotFound = onNotFound;
    }

    public static ResolvingProxyResolver of(Catalog catalog) {
        return new ResolvingProxyResolver(catalog);
    }

    public @Override CatalogInfo apply(CatalogInfo t) {
        return resolve(t);
    }

    @SuppressWarnings("unchecked")
    public <T extends CatalogInfo> T resolve(final T orig) {
        if (orig == null) {
            return null;
        }

        final ResolvingProxy resolvingProxy = getResolvingProxy(orig);
        final boolean isResolvingProxy = null != resolvingProxy;
        if (isResolvingProxy) {
            // may the object itself be a resolving proxy
            T info = ResolvingProxy.resolve(catalog, orig);
            if (info == null) {
                onNotFound.accept(orig, resolvingProxy);
                // return the proxied value if the consumer didn't throw an exception
                return orig;
            }
            return info;
        }

        if (orig instanceof StyleInfo) return (T) resolveInternal((StyleInfo) orig);

        if (orig instanceof PublishedInfo) return (T) resolveInternal((PublishedInfo) orig);

        if (orig instanceof ResourceInfo) return (T) resolveInternal((ResourceInfo) orig);

        if (orig instanceof StoreInfo) return (T) resolveInternal((StoreInfo) orig);

        return orig;
    }

    protected boolean isResolvingProxy(final CatalogInfo unresolved) {
        return getResolvingProxy(unresolved) != null;
    }

    protected ResolvingProxy getResolvingProxy(final CatalogInfo unresolved) {
        if (unresolved != null) {
            boolean isProxy = Proxy.isProxyClass(unresolved.getClass());
            if (isProxy) {
                InvocationHandler invocationHandler = Proxy.getInvocationHandler(unresolved);
                if (invocationHandler instanceof ResolvingProxy) {
                    return (ResolvingProxy) invocationHandler;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected <T extends PublishedInfo> T resolveInternal(T published) {
        if (published instanceof LayerInfo) return (T) resolveInternal((LayerInfo) published);

        if (published instanceof LayerGroupInfo)
            return (T) resolveInternal((LayerGroupInfo) published);

        return published;
    }

    protected LayerInfo resolveInternal(LayerInfo layer) {
        if (isResolvingProxy(layer.getResource())) layer.setResource(resolve(layer.getResource()));

        if (isResolvingProxy(layer.getDefaultStyle()))
            layer.setDefaultStyle(resolve(layer.getDefaultStyle()));

        final boolean hasProxiedStyles =
                layer.getStyles().stream().anyMatch(this::isResolvingProxy);
        if (hasProxiedStyles) {

            LinkedHashSet<StyleInfo> resolvedStyles;
            resolvedStyles =
                    layer.getStyles()
                            .stream()
                            .map(this::resolve)
                            .collect(Collectors.toCollection(LinkedHashSet::new));

            layer.getStyles().clear();
            layer.getStyles().addAll(resolvedStyles);
        }
        return layer;
    }

    protected LayerGroupInfo resolveInternal(LayerGroupInfo lg) {
        for (int i = 0; i < lg.getLayers().size(); i++) {
            PublishedInfo l = lg.getLayers().get(i);
            if (l != null) {
                lg.getLayers().set(i, resolve(l));
            }
        }

        for (int i = 0; i < lg.getStyles().size(); i++) {
            StyleInfo s = lg.getStyles().get(i);
            if (s != null) {
                lg.getStyles().set(i, resolve(s));
            }
        }
        lg.setWorkspace(resolve(lg.getWorkspace()));
        return lg;
    }

    protected StyleInfo resolveInternal(StyleInfo style) {
        // resolve the workspace
        WorkspaceInfo ws = style.getWorkspace();
        if (isResolvingProxy(ws)) {
            style.setWorkspace(resolve(ws));
        }
        return style;
    }

    protected StoreInfo resolveInternal(StoreInfo store) {
        // resolve the workspace
        WorkspaceInfo ws = store.getWorkspace();
        if (isResolvingProxy(ws)) {
            store.setWorkspace(resolve(ws));
        }
        return store;
    }

    protected ResourceInfo resolveInternal(ResourceInfo resource) {
        // resolve the store
        StoreInfo store = resource.getStore();
        if (isResolvingProxy(store)) {
            resource.setStore(resolve(store));
        }

        // resolve the namespace
        NamespaceInfo namespace = resource.getNamespace();
        if (isResolvingProxy(namespace)) {
            resource.setNamespace(resolve(namespace));
        }
        return resource;
    }
}
