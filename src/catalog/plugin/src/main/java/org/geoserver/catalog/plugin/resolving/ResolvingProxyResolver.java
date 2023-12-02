/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Catalog;
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
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * {@link ResolvingCatalogFacadeDecorator#setObjectResolver resolving function} that resolves {@link
 * CatalogInfo} properties that are proxied through {@link ResolvingProxy} before returning the
 * object from the facade.
 *
 * <p>When resolving object references from a stream of objects, it's convenient to use the {@link
 * #memoizing() memoizing} supplier, which will keep a local cache during the lifetime of the stream
 * to avoid querying the catalog over repeated occurrences. Note though, this may not be necessary
 * at if the catalog can do very fast id lookups. For example, if it has its own caching mechanism
 * or is a purely in-memory catalog.
 *
 * @see ResolvingProxy
 */
@Slf4j
public class ResolvingProxyResolver<T extends Info> implements UnaryOperator<T> {

    private final Catalog catalog;
    private final BiConsumer<CatalogInfo, ResolvingProxy> onNotFound;
    private final ProxyUtils proxyUtils;

    public ResolvingProxyResolver(Catalog catalog) {
        this(
                catalog,
                (info, proxy) -> {
                    log.warn(
                            "ResolvingProxy object not found in catalog, keeping proxy around: "
                                    + info.getId());
                });
    }

    public ResolvingProxyResolver(
            Catalog catalog, BiConsumer<CatalogInfo, ResolvingProxy> onNotFound) {
        requireNonNull(catalog);
        requireNonNull(onNotFound);
        this.catalog = catalog;
        this.onNotFound = onNotFound;
        this.proxyUtils = new ProxyUtils(catalog, Optional.empty());
    }

    public static <I extends Info> ResolvingProxyResolver<I> of(
            Catalog catalog, BiConsumer<CatalogInfo, ResolvingProxy> onNotFound) {
        return new ResolvingProxyResolver<>(catalog, onNotFound);
    }

    public static <I extends Info> ResolvingProxyResolver<I> of(
            Catalog catalog, boolean errorOnNotFound) {
        if (errorOnNotFound)
            return ResolvingProxyResolver.of(
                    catalog,
                    (proxiedInfo, proxy) -> {
                        throw new NoSuchElementException(
                                "Object not found: " + proxiedInfo.getId());
                    });
        return ResolvingProxyResolver.of(catalog);
    }

    public static <I extends Info> ResolvingProxyResolver<I> of(Catalog catalog) {
        return new ResolvingProxyResolver<>(catalog);
    }

    @SuppressWarnings("unchecked")
    public <I extends Info> ResolvingProxyResolver<I> memoizing() {
        return (ResolvingProxyResolver<I>) new MemoizingProxyResolver(catalog, onNotFound);
    }

    @Override
    public T apply(T info) {
        return resolve(info);
    }

    @SuppressWarnings("unchecked")
    public <I extends Info> I resolve(final I orig) {
        if (orig == null) {
            return null;
        }

        final ResolvingProxy resolvingProxy = getResolvingProxy(orig);
        final boolean isResolvingProxy = null != resolvingProxy;
        if (isResolvingProxy) {
            // may the object itself be a resolving proxy
            I resolved = doResolveProxy(orig);
            if (resolved == null && orig instanceof CatalogInfo) {
                log.info("Proxy object {} not found, calling on-not-found consumer", orig.getId());
                onNotFound.accept((CatalogInfo) orig, resolvingProxy);
                // return the proxied value if the consumer didn't throw an exception
                return orig;
            }
            return resolved;
        }

        if (orig instanceof StyleInfo) return (I) resolveInternal((StyleInfo) orig);

        if (orig instanceof PublishedInfo) return (I) resolveInternal((PublishedInfo) orig);

        if (orig instanceof ResourceInfo) return (I) resolveInternal((ResourceInfo) orig);

        if (orig instanceof StoreInfo) return (I) resolveInternal((StoreInfo) orig);

        if (orig instanceof SettingsInfo) return (I) resolveInternal((SettingsInfo) orig);

        if (orig instanceof ServiceInfo) return (I) resolveInternal((ServiceInfo) orig);

        return orig;
    }

    protected <I extends Info> I doResolveProxy(final I orig) {
        return proxyUtils.resolve(orig);
    }

    protected boolean isResolvingProxy(final CatalogInfo unresolved) {
        return getResolvingProxy(unresolved) != null;
    }

    protected ResolvingProxy getResolvingProxy(final Info unresolved) {
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
    protected <P extends PublishedInfo> P resolveInternal(P published) {
        if (published instanceof LayerInfo) return (P) resolveInternal((LayerInfo) published);

        if (published instanceof LayerGroupInfo)
            return (P) resolveInternal((LayerGroupInfo) published);

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
                    layer.getStyles().stream()
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
        lg.setRootLayer(resolve(lg.getRootLayer()));
        lg.setRootLayerStyle(resolve(lg.getRootLayerStyle()));
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

    protected SettingsInfo resolveInternal(SettingsInfo settings) {
        settings.setWorkspace(resolve(settings.getWorkspace()));
        return settings;
    }

    protected ServiceInfo resolveInternal(ServiceInfo service) {
        service.setWorkspace(resolve(service.getWorkspace()));
        return service;
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

    private static class MemoizingProxyResolver extends ResolvingProxyResolver<Info> {

        private Map<String, Info> resolved = new ConcurrentHashMap<>();

        public MemoizingProxyResolver(
                Catalog catalog, BiConsumer<CatalogInfo, ResolvingProxy> onNotFound) {
            super(catalog, onNotFound);
        }

        @SuppressWarnings("unchecked")
        protected @Override <I extends Info> I doResolveProxy(final I orig) {
            String id = orig.getId();
            I resolved = (I) this.resolved.get(id);
            if (null == resolved) {
                log.trace("Memoized cache miss, resolving proxy reference {}", id);
                resolved = (I) this.resolved.computeIfAbsent(id, key -> super.doResolveProxy(orig));
            } else {
                log.trace("Memoized cache hit for {}", resolved.getId());
            }
            return resolved;
        }
    }
}
