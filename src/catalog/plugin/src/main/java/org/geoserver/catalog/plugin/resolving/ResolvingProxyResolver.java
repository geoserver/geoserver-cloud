/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.resolving;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import lombok.NonNull;
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
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

/**
 * A {@link UnaryOperator} that resolves {@link ResolvingProxy} references within {@link CatalogInfo} objects.
 *
 * <p>This utility resolves proxied properties in {@link CatalogInfo} objects (e.g., {@link StoreInfo#getWorkspace()},
 * {@link ResourceInfo#getNamespace()}) by fetching actual instances from a supplied {@link Catalog}, suitable
 * for use with {@link ResolvingCatalogFacadeDecorator#setOutboundResolver(UnaryOperator)}. It processes both top-level
 * proxies and nested references recursively, with configurable behavior for unresolved proxies via a
 * {@link BiConsumer} callback. A memoizing variant is available via {@link #memoizing()} for efficient stream
 * processing.
 *
 * <p>Key features:
 * <ul>
 *   <li><strong>Proxy Resolution:</strong> Replaces {@link ResolvingProxy} instances with catalog objects.</li>
 *   <li><strong>Recursive Handling:</strong> Resolves nested references in complex types (e.g., {@link LayerGroupInfo}).</li>
 *   <li><strong>Configurable Failure:</strong> Allows customization of unresolved proxy handling.</li>
 *   <li><strong>Memoization Option:</strong> Offers a caching variant for repeated lookups.</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * Catalog catalog = ...;
 * ResolvingProxyResolver<CatalogInfo> resolver = ResolvingProxyResolver.of(catalog);
 * ResolvingCatalogFacadeDecorator facade = ...;
 * facade.setOutboundResolver(resolver);
 * </pre>
 *
 * <p>When resolving object references from a stream of objects, it’s convenient to use the {@link #memoizing()}
 * supplier, which will keep a local cache during the lifetime of the stream to avoid querying the catalog over
 * repeated occurrences. Note though, this may not be necessary if the catalog can do very fast id lookups—for
 * example, if it has its own caching mechanism or is a purely in-memory catalog.
 *
 * @param <T> The type of object to resolve (typically {@link CatalogInfo} or its subtypes).
 * @since 1.0
 * @see ResolvingProxy
 * @see ResolvingCatalogFacadeDecorator
 */
@Slf4j(topic = "org.geoserver.catalog.plugin.resolving")
public class ResolvingProxyResolver<T> implements UnaryOperator<T> {

    private final Supplier<Catalog> catalog;
    private final ProxyUtils proxyUtils;

    private BiConsumer<CatalogInfo, ResolvingProxy> onNotFound;

    /**
     * Constructs a resolver with a catalog supplier and default not-found behavior.
     *
     * <p>Uses a logging warning as the default action for unresolved proxies.
     *
     * @param catalog The supplier of the {@link Catalog} for resolution; must not be null.
     * @throws NullPointerException if {@code catalog} is null.
     */
    private ResolvingProxyResolver(@NonNull Supplier<Catalog> catalog) {
        this(
                catalog,
                (info, proxy) -> log.warn("ResolvingProxy object not found in catalog, keeping proxy around: %s"
                        .formatted(info.getId())));
    }

    /**
     * Constructs a resolver with a catalog supplier and custom not-found behavior.
     *
     * @param catalog   The supplier of the {@link Catalog} for resolution; must not be null.
     * @param onNotFound The action to take when a proxy cannot be resolved; must not be null.
     * @throws NullPointerException if {@code catalog} or {@code onNotFound} is null.
     */
    private ResolvingProxyResolver(
            @NonNull Supplier<Catalog> catalog, @NonNull BiConsumer<CatalogInfo, ResolvingProxy> onNotFound) {
        Objects.requireNonNull(catalog, "Catalog supplier must not be null");
        Objects.requireNonNull(onNotFound, "onNotFound consumer must not be null");
        this.catalog = catalog;
        this.onNotFound = onNotFound;
        this.proxyUtils = new ProxyUtils(catalog, Optional.empty());
    }

    /**
     * Creates a resolver with a fixed catalog and custom not-found behavior.
     *
     * @param <I>        The type of {@link Info} to resolve.
     * @param catalog    The {@link Catalog} to use; must not be null.
     * @param onNotFound The action for unresolved proxies; must not be null.
     * @return A new {@link ResolvingProxyResolver} instance.
     * @throws NullPointerException if {@code catalog} or {@code onNotFound} is null.
     */
    public static <I extends Info> ResolvingProxyResolver<I> of(
            Catalog catalog, BiConsumer<CatalogInfo, ResolvingProxy> onNotFound) {
        return new ResolvingProxyResolver<>(() -> catalog, onNotFound);
    }

    /**
     * Creates a resolver with a catalog supplier and custom not-found behavior.
     *
     * @param <I>        The type of {@link Info} to resolve.
     * @param catalog    The supplier of the {@link Catalog}; must not be null.
     * @param onNotFound The action for unresolved proxies; must not be null.
     * @return A new {@link ResolvingProxyResolver} instance.
     * @throws NullPointerException if {@code catalog} or {@code onNotFound} is null.
     */
    public static <I extends Info> ResolvingProxyResolver<I> of(
            Supplier<Catalog> catalog, BiConsumer<CatalogInfo, ResolvingProxy> onNotFound) {
        return new ResolvingProxyResolver<>(catalog, onNotFound);
    }

    /**
     * Creates a resolver with a fixed catalog and configurable failure on not found.
     *
     * @param <I>           The type of {@link Info} to resolve.
     * @param catalog       The {@link Catalog} to use; must not be null.
     * @param errorOnNotFound If true, throws an exception on unresolved proxies; if false, logs a warning.
     * @return A new {@link ResolvingProxyResolver} instance.
     * @throws NullPointerException if {@code catalog} is null.
     */
    public static <I extends Info> ResolvingProxyResolver<I> of(Catalog catalog, boolean errorOnNotFound) {
        if (errorOnNotFound) {
            return ResolvingProxyResolver.of(catalog, (proxiedInfo, proxy) -> {
                throw new NoSuchElementException("Object not found: %s".formatted(proxiedInfo.getId()));
            });
        }
        return ResolvingProxyResolver.of(catalog);
    }

    /**
     * Creates a resolver with a fixed catalog and default not-found behavior.
     *
     * @param <I>     The type of {@link Info} to resolve.
     * @param catalog The {@link Catalog} to use; must not be null.
     * @return A new {@link ResolvingProxyResolver} instance.
     * @throws NullPointerException if {@code catalog} is null.
     */
    public static <I extends Info> ResolvingProxyResolver<I> of(Catalog catalog) {
        return new ResolvingProxyResolver<>(() -> catalog);
    }

    /**
     * Creates a resolver with a catalog supplier and default not-found behavior.
     *
     * @param <I>     The type of {@link Info} to resolve.
     * @param catalog The supplier of the {@link Catalog}; must not be null.
     * @return A new {@link ResolvingProxyResolver} instance.
     * @throws NullPointerException if {@code catalog} is null.
     */
    public static <I extends Info> ResolvingProxyResolver<I> of(Supplier<Catalog> catalog) {
        return new ResolvingProxyResolver<>(catalog);
    }

    /**
     * Creates a memoizing version of this resolver.
     *
     * <p>The memoizing variant caches resolved objects by ID to optimize repeated lookups, useful for streams.
     *
     * @param <I> The type of {@link Info} to resolve.
     * @return A memoizing {@link ResolvingProxyResolver}.
     */
    @SuppressWarnings("unchecked")
    public <I extends Info> ResolvingProxyResolver<I> memoizing() {
        return (ResolvingProxyResolver<I>) new MemoizingProxyResolver(catalog, onNotFound);
    }

    /**
     * Configures the action for unresolved proxies.
     *
     * @param onNotFound The action to take when a proxy cannot be resolved; must not be null.
     * @return This resolver instance for chaining.
     * @throws NullPointerException if {@code onNotFound} is null.
     */
    public ResolvingProxyResolver<T> onNotFound(BiConsumer<CatalogInfo, ResolvingProxy> onNotFound) {
        Objects.requireNonNull(onNotFound, "onNotFound consumer must not be null");
        this.onNotFound = onNotFound;
        return this;
    }

    /**
     * Applies the resolver to process an object, resolving any {@link ResolvingProxy} references.
     *
     * @param info The object to resolve; may be null.
     * @return The resolved object, or null if {@code info} is null.
     */
    @Override
    public T apply(T info) {
        return resolve(info);
    }

    /**
     * Resolves an object, handling top-level and nested {@link ResolvingProxy} references.
     *
     * <p>If the object is a {@link ResolvingProxy}, resolves it via the catalog; otherwise, processes nested
     * references based on type (e.g., {@link LayerInfo}, {@link StoreInfo}). Unresolved proxies trigger the
     * configured {@code onNotFound} action, defaulting to keeping the proxy if no exception is thrown.
     *
     * @param <I>  The type of object to resolve.
     * @param orig The object to resolve; may be null.
     * @return The resolved object, or {@code orig} if unresolved and allowed by {@code onNotFound}.
     */
    @SuppressWarnings("unchecked")
    public <I> I resolve(final I orig) {
        if (orig == null) {
            return null;
        }

        if (orig instanceof Info info) {
            final ResolvingProxy resolvingProxy = getResolvingProxy(info);
            final boolean isResolvingProxy = null != resolvingProxy;
            if (isResolvingProxy) {
                // may the object itself be a resolving proxy
                Info resolved = doResolveProxy(info);
                if (resolved == null && info instanceof CatalogInfo cinfo) {
                    log.debug("Proxy object {} not found, calling on-not-found consumer", info.getId());
                    onNotFound.accept(cinfo, resolvingProxy);
                    // if onNotFound didn't throw an exception, return the proxied value if the
                    // consumer didn't throw an exception
                    return orig;
                }
                return (I) resolved;
            }
        }

        if (orig instanceof StyleInfo style) {
            return (I) resolveInternal(style);
        }

        if (orig instanceof PublishedInfo published) {
            return (I) resolveInternal(published);
        }

        if (orig instanceof ResourceInfo resource) {
            return (I) resolveInternal(resource);
        }

        if (orig instanceof StoreInfo store) {
            return (I) resolveInternal(store);
        }

        if (orig instanceof SettingsInfo settings) {
            return (I) resolveInternal(settings);
        }

        if (orig instanceof ServiceInfo service) {
            return (I) resolveInternal(service);
        }

        if (orig instanceof Patch patch) {
            return (I) resolveInternal(patch);
        }

        return orig;
    }

    /**
     * Resolves a {@link Patch} using internal utilities.
     *
     * @param patch The {@link Patch} to resolve; must not be null.
     * @return The resolved {@link Patch}.
     */
    private Patch resolveInternal(Patch patch) {
        return proxyUtils.resolve(patch);
    }

    /**
     * Performs the actual proxy resolution using {@link ProxyUtils}.
     *
     * @param <I>  The type of {@link Info}.
     * @param orig The proxy object to resolve; must not be null.
     * @return The resolved object, or null if not found.
     */
    protected <I extends Info> I doResolveProxy(final I orig) {
        return proxyUtils.resolve(orig);
    }

    /**
     * Checks if an object is a {@link ResolvingProxy}.
     *
     * @param unresolved The {@link CatalogInfo} to check; may be null.
     * @return {@code true} if it’s a {@link ResolvingProxy}, {@code false} otherwise.
     */
    protected boolean isResolvingProxy(final CatalogInfo unresolved) {
        return getResolvingProxy(unresolved) != null;
    }

    /**
     * Retrieves the {@link ResolvingProxy} handler if present.
     *
     * @param unresolved The {@link Info} to check; may be null.
     * @return The {@link ResolvingProxy} handler, or null if not a proxy or not a {@link ResolvingProxy}.
     */
    protected ResolvingProxy getResolvingProxy(final Info unresolved) {
        if (unresolved != null) {
            boolean isProxy = Proxy.isProxyClass(unresolved.getClass());
            if (isProxy) {
                InvocationHandler invocationHandler = Proxy.getInvocationHandler(unresolved);
                if (invocationHandler instanceof ResolvingProxy resolvingProxy) {
                    return resolvingProxy;
                }
            }
        }
        return null;
    }

    /**
     * Resolves nested references in a {@link PublishedInfo} polymorphically.
     *
     * @param <P>       The type of {@link PublishedInfo}.
     * @param published The {@link PublishedInfo} to resolve; must not be null.
     * @return The resolved {@link PublishedInfo}.
     */
    @SuppressWarnings("unchecked")
    protected <P extends PublishedInfo> P resolveInternal(P published) {
        if (published instanceof LayerInfo layer) {
            return (P) resolveInternal(layer);
        }

        if (published instanceof LayerGroupInfo lg) {
            return (P) resolveInternal(lg);
        }

        return published;
    }

    /**
     * Resolves nested references in a {@link LayerInfo}.
     *
     * @param layer The {@link LayerInfo} to resolve; must not be null.
     * @return The resolved {@link LayerInfo}.
     */
    protected LayerInfo resolveInternal(LayerInfo layer) {
        if (isResolvingProxy(layer.getResource())) {
            layer.setResource(resolve(layer.getResource()));
        }

        if (isResolvingProxy(layer.getDefaultStyle())) {
            layer.setDefaultStyle(resolve(layer.getDefaultStyle()));
        }

        final boolean hasProxiedStyles = layer.getStyles().stream().anyMatch(this::isResolvingProxy);
        if (hasProxiedStyles) {
            LinkedHashSet<StyleInfo> resolvedStyles =
                    layer.getStyles().stream().map(this::resolve).collect(Collectors.toCollection(LinkedHashSet::new));
            layer.getStyles().clear();
            layer.getStyles().addAll(resolvedStyles);
        }
        return layer;
    }

    /**
     * Resolves nested references in a {@link LayerGroupInfo}.
     *
     * @param lg The {@link LayerGroupInfo} to resolve; must not be null.
     * @return The resolved {@link LayerGroupInfo}.
     */
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

    /**
     * Resolves nested references in a {@link StyleInfo}.
     *
     * @param style The {@link StyleInfo} to resolve; must not be null.
     * @return The resolved {@link StyleInfo}.
     */
    protected StyleInfo resolveInternal(StyleInfo style) {
        // resolve the workspace
        WorkspaceInfo ws = style.getWorkspace();
        if (isResolvingProxy(ws)) {
            style.setWorkspace(resolve(ws));
        }
        return style;
    }

    /**
     * Resolves nested references in a {@link StoreInfo}.
     *
     * @param store The {@link StoreInfo} to resolve; must not be null.
     * @return The resolved {@link StoreInfo}.
     */
    protected StoreInfo resolveInternal(StoreInfo store) {
        // resolve the workspace
        WorkspaceInfo ws = store.getWorkspace();
        if (isResolvingProxy(ws)) {
            store.setWorkspace(resolve(ws));
        }
        return store;
    }

    /**
     * Resolves nested references in a {@link SettingsInfo}.
     *
     * @param settings The {@link SettingsInfo} to resolve; must not be null.
     * @return The resolved {@link SettingsInfo}.
     */
    protected SettingsInfo resolveInternal(SettingsInfo settings) {
        settings.setWorkspace(resolve(settings.getWorkspace()));
        return settings;
    }

    /**
     * Resolves nested references in a {@link ServiceInfo}.
     *
     * @param service The {@link ServiceInfo} to resolve; must not be null.
     * @return The resolved {@link ServiceInfo}.
     */
    protected ServiceInfo resolveInternal(ServiceInfo service) {
        service.setWorkspace(resolve(service.getWorkspace()));
        return service;
    }

    /**
     * Resolves nested references in a {@link ResourceInfo}.
     *
     * @param resource The {@link ResourceInfo} to resolve; must not be null.
     * @return The resolved {@link ResourceInfo}.
     */
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

    /**
     * A memoizing variant of {@link ResolvingProxyResolver} that caches resolved objects.
     */
    private static class MemoizingProxyResolver extends ResolvingProxyResolver<Info> {

        private Map<String, Info> resolvedById = new ConcurrentHashMap<>();

        /**
         * Constructs a memoizing resolver with a catalog supplier and not-found behavior.
         *
         * @param catalog    The supplier of the {@link Catalog}; must not be null.
         * @param onNotFound The action for unresolved proxies; must not be null.
         */
        public MemoizingProxyResolver(
                @NonNull Supplier<Catalog> catalog, BiConsumer<CatalogInfo, ResolvingProxy> onNotFound) {
            super(catalog, onNotFound);
        }

        /**
         * Resolves a proxy, using the cache to avoid repeated lookups.
         *
         * @param <I>  The type of {@link Info}.
         * @param orig The proxy object to resolve; must not be null.
         * @return The resolved object, or null if not found.
         */
        @SuppressWarnings("unchecked")
        protected @Override <I extends Info> I doResolveProxy(final I orig) {
            String id = orig.getId();
            I resolved = (I) this.resolvedById.get(id);
            if (null == resolved) {
                log.trace("Memoized cache miss, resolving proxy reference {}", id);
                resolved = computeIfAbsent(orig);
            } else {
                log.trace("Memoized cache hit for {}", resolved.getId());
            }
            return resolved;
        }

        /**
         * Computes and caches the resolved object if absent.
         *
         * @param <I>  The type of {@link Info}.
         * @param orig The proxy object to resolve; must not be null.
         * @return The resolved object.
         */
        @SuppressWarnings("unchecked")
        private <I extends Info> I computeIfAbsent(final I orig) {
            return (I) resolvedById.computeIfAbsent(orig.getId(), key -> super.doResolveProxy(orig));
        }
    }
}
