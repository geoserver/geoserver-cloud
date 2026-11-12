/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.resolving;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
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
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

/**
 * A {@link UnaryOperator} that resolves {@link ResolvingProxy} references within {@link CatalogInfo} objects.
 *
 * <p>This utility resolves proxied properties in {@link CatalogInfo} objects (e.g., {@link StoreInfo#getWorkspace()},
 * {@link ResourceInfo#getNamespace()}) by fetching actual instances from a supplied {@link Catalog}, suitable for use
 * with {@link ResolvingCatalogFacadeDecorator#setOutboundResolver(UnaryOperator)}. It processes both top-level proxies
 * and nested references recursively, with configurable behavior for unresolved proxies via a {@link BiConsumer}
 * callback. A memoizing variant is available via {@link #memoizing()} for efficient stream processing.
 *
 * <p>{@link #of(ExtendedCatalogFacade)} looks the references up in a catalog facade instead of a catalog. A decorator
 * of that facade then resolves its objects below the workspace isolation of the catalog.
 *
 * <p>Through a facade, a call to {@link #resolve(Object)} remembers the objects found for its proxies, by the type and
 * the id of the proxy. Another proxy of the same type and id gets the object without a lookup: the layers of a group
 * share their store, and the store is looked up for the first layer only. An object brought by the row of another
 * object is not a proxy, and does not enter the memo. An object gets its references resolved once in the call. The memo
 * lives as long as the call: the resolver keeps no cache.
 *
 * <p>A reference is written back to its object only if it resolves to another instance, as a proxy does. The styles set
 * of a layer is also refilled if it misses one of its styles, after a style resolved in place changed its hash. A
 * second pass over a resolved object then writes nothing, and other threads may read the object meanwhile.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li><strong>Proxy Resolution:</strong> Replaces {@link ResolvingProxy} instances with catalog objects.
 *   <li><strong>Recursive Handling:</strong> Resolves nested references in complex types (e.g.,
 *       {@link LayerGroupInfo}).
 *   <li><strong>Configurable Failure:</strong> Allows customization of unresolved proxy handling.
 *   <li><strong>Memoization Option:</strong> Offers a caching variant for repeated lookups.
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Catalog catalog = ...;
 * ResolvingProxyResolver<CatalogInfo> resolver = ResolvingProxyResolver.of(catalog);
 * ResolvingCatalogFacadeDecorator facade = ...;
 * facade.setOutboundResolver(resolver);
 * }</pre>
 *
 * <p>When resolving object references from a stream of objects, it’s convenient to use the {@link #memoizing()}
 * supplier, which will keep a local cache during the lifetime of the stream to avoid querying the catalog over repeated
 * occurrences. Note though, this may not be necessary if the catalog can do very fast id lookups—for example, if it has
 * its own caching mechanism or is a purely in-memory catalog.
 *
 * @param <T> The type of object to resolve (typically {@link CatalogInfo} or its subtypes).
 * @since 1.0
 * @see ResolvingProxy
 * @see ResolvingCatalogFacadeDecorator
 */
@Slf4j(topic = "org.geoserver.catalog.plugin.resolving")
public class ResolvingProxyResolver<T> implements UnaryOperator<T> {

    private final ProxyUtils proxyUtils;

    /**
     * Whether references are looked up in a facade below the catalog. Such a facade returns raw objects. The object
     * found for a reference may hold proxies, as may an object referenced directly, such as a style read with its
     * layer. The resolver resolves them in turn. The catalog returns objects with their references resolved, and other
     * threads may share them: the resolver leaves them as they are.
     *
     * <p>The layer group styles of a group are resolved through a facade only. The catalog path leaves them as they
     * are.
     */
    private final boolean throughFacade;

    private BiConsumer<Info, ResolvingProxy> onNotFound;

    /**
     * Constructs a resolver with a catalog supplier and default not-found behavior.
     *
     * <p>Uses a logging warning as the default action for unresolved proxies.
     *
     * @param catalog The supplier of the {@link Catalog} for resolution; must not be null.
     * @throws NullPointerException if {@code catalog} is null.
     */
    private ResolvingProxyResolver(@NonNull Supplier<Catalog> catalog) {
        this(catalog, ResolvingProxyResolver::warnNotFound);
    }

    /**
     * Constructs a resolver with a catalog supplier and custom not-found behavior.
     *
     * @param catalog The supplier of the {@link Catalog} for resolution; must not be null.
     * @param onNotFound The action to take when a proxy cannot be resolved; must not be null.
     * @throws NullPointerException if {@code catalog} or {@code onNotFound} is null.
     */
    private ResolvingProxyResolver(
            @NonNull Supplier<Catalog> catalog, @NonNull BiConsumer<Info, ResolvingProxy> onNotFound) {
        this(new ProxyUtils(catalog, Optional.empty()), false, onNotFound);
    }

    private ResolvingProxyResolver(
            @NonNull ProxyUtils proxyUtils,
            boolean throughFacade,
            @NonNull BiConsumer<Info, ResolvingProxy> onNotFound) {
        this.proxyUtils = proxyUtils;
        this.throughFacade = throughFacade;
        this.onNotFound = onNotFound;
    }

    private static void warnNotFound(Info info, ResolvingProxy proxy) {
        log.warn("ResolvingProxy object not found in catalog, keeping proxy around: %s".formatted(info.getId()));
    }

    /**
     * Creates a resolver looking references up in a facade instead of a catalog, see {@link FacadeReferenceLookup}. The
     * facade returns raw objects, and the resolver resolves the references of the objects found, in turn. The members
     * of a layer group come with one call to the facade, and its styles with another.
     *
     * <p>A {@link ResolvingCatalogFacadeDecorator} placed below the workspace isolation of the catalog uses it to
     * resolve the references of the objects of its decorated facade through that same facade.
     *
     * @param <I> The type of {@link Info} to resolve.
     * @param facade The facade holding the referenced objects; must not be null.
     * @return A new {@link ResolvingProxyResolver} instance.
     */
    public static <I extends Info> ResolvingProxyResolver<I> of(@NonNull ExtendedCatalogFacade facade) {
        ProxyUtils facadeLookups = new ProxyUtils(new FacadeReferenceLookup(facade));
        return new ResolvingProxyResolver<>(facadeLookups, true, ResolvingProxyResolver::warnNotFound);
    }

    /**
     * Creates a resolver with a fixed catalog and custom not-found behavior.
     *
     * @param <I> The type of {@link Info} to resolve.
     * @param catalog The {@link Catalog} to use; must not be null.
     * @param onNotFound The action for unresolved proxies; must not be null.
     * @return A new {@link ResolvingProxyResolver} instance.
     * @throws NullPointerException if {@code catalog} or {@code onNotFound} is null.
     */
    public static <I extends Info> ResolvingProxyResolver<I> of(
            Catalog catalog, BiConsumer<Info, ResolvingProxy> onNotFound) {
        return new ResolvingProxyResolver<>(() -> catalog, onNotFound);
    }

    /**
     * Creates a resolver with a catalog supplier and custom not-found behavior.
     *
     * @param <I> The type of {@link Info} to resolve.
     * @param catalog The supplier of the {@link Catalog}; must not be null.
     * @param onNotFound The action for unresolved proxies; must not be null.
     * @return A new {@link ResolvingProxyResolver} instance.
     * @throws NullPointerException if {@code catalog} or {@code onNotFound} is null.
     */
    public static <I extends Info> ResolvingProxyResolver<I> of(
            Supplier<Catalog> catalog, BiConsumer<Info, ResolvingProxy> onNotFound) {
        return new ResolvingProxyResolver<>(catalog, onNotFound);
    }

    /**
     * Creates a resolver with a fixed catalog and configurable failure on not found.
     *
     * @param <I> The type of {@link Info} to resolve.
     * @param catalog The {@link Catalog} to use; must not be null.
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
     * @param <I> The type of {@link Info} to resolve.
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
     * @param <I> The type of {@link Info} to resolve.
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
        return (ResolvingProxyResolver<I>) new MemoizingProxyResolver(proxyUtils, throughFacade, onNotFound);
    }

    /**
     * Configures the action for unresolved proxies.
     *
     * @param onNotFound The action to take when a proxy cannot be resolved; must not be null.
     * @return This resolver instance for chaining.
     * @throws NullPointerException if {@code onNotFound} is null.
     */
    public ResolvingProxyResolver<T> onNotFound(BiConsumer<Info, ResolvingProxy> onNotFound) {
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
     * <p>If the object is a {@link ResolvingProxy}, resolves it via the catalog; otherwise, processes nested references
     * based on type (e.g., {@link LayerInfo}, {@link StoreInfo}). Unresolved proxies trigger the configured
     * {@code onNotFound} action, defaulting to keeping the proxy if no exception is thrown.
     *
     * @param <I> The type of object to resolve.
     * @param orig The object to resolve; may be null.
     * @return The resolved object, or {@code orig} if unresolved and allowed by {@code onNotFound}.
     */
    public <I> I resolve(final I orig) {
        return resolve(orig, newCallMemo());
    }

    /**
     * A memo for one call through a facade. The catalog path keeps none, and looks a reference up where it meets it.
     */
    private CallMemo newCallMemo() {
        if (throughFacade) {
            return new FacadeCallMemo();
        }
        return CallMemo.NONE;
    }

    /**
     * Resolves an object within a call. A proxy is replaced by the referenced object. An object other than a proxy gets
     * its own references resolved, unless the call met it already.
     */
    @SuppressWarnings("unchecked")
    private <I> I resolve(final I orig, CallMemo memo) {
        if (orig == null) {
            return null;
        }
        if (orig instanceof Info info) {
            ResolvingProxy handler = getResolvingProxy(info);
            if (handler != null) {
                return (I) resolveProxy(info, handler, memo);
            }
            if (!memo.firstVisit(info)) {
                return orig;
            }
        }
        return resolveReferencesOf(orig, memo);
    }

    /**
     * Resolves a proxy to the referenced object, with the references of that object resolved in turn. The memo holds
     * the objects found earlier in the call, by a lookup or by a bulk call, and a proxy pointing to one of them needs
     * no lookup. A proxy not found stays, after the not-found action.
     */
    private Info resolveProxy(Info proxy, ResolvingProxy handler, CallMemo memo) {
        Reference reference = Reference.of(proxy, handler);
        if (memo.isNotFound(reference)) {
            return proxy;
        }
        Info found = memo.found(reference);
        if (found == null) {
            return lookUp(proxy, handler, memo);
        }
        // a bulk call puts its objects in the memo before their references are resolved
        return resolveFound(found, memo);
    }

    /**
     * Looks the reference of a proxy up, and resolves the references of the object found. The memo gets the object
     * before its references are resolved. The catalog rejects a group containing itself, and the lookups would end even
     * if one got through.
     */
    private Info lookUp(Info proxy, ResolvingProxy handler, CallMemo memo) {
        Info found = doResolveProxy(proxy);
        if (found == null || isResolvingProxy(found)) {
            return notFound(proxy, handler, memo);
        }
        memo.rememberFound(Reference.of(proxy, handler), found);
        return resolveFound(found, memo);
    }

    private Info notFound(Info proxy, ResolvingProxy handler, CallMemo memo) {
        log.debug("Proxy object {} not found, calling on-not-found consumer", proxy.getId());
        memo.rememberNotFound(Reference.of(proxy, handler));
        onNotFound.accept(proxy, handler);
        // the consumer did not throw: the proxy stays
        return proxy;
    }

    /**
     * Resolves the references of an object found for a reference, if it comes from a facade. An object found through
     * the catalog comes with its references resolved.
     */
    private <I> I resolveFound(I found, CallMemo memo) {
        if (throughFacade) {
            return resolve(found, memo);
        }
        return found;
    }

    /** Resolves the references held by an object other than a proxy, according to its type. */
    @SuppressWarnings("unchecked")
    private <I> I resolveReferencesOf(I object, CallMemo memo) {
        if (object instanceof StyleInfo style) {
            return (I) resolveInternal(style, memo);
        }
        if (object instanceof PublishedInfo published) {
            return (I) resolveInternal(published, memo);
        }
        if (object instanceof ResourceInfo resource) {
            return (I) resolveInternal(resource, memo);
        }
        if (object instanceof StoreInfo store) {
            return (I) resolveInternal(store, memo);
        }
        if (object instanceof SettingsInfo settings) {
            return (I) resolveInternal(settings, memo);
        }
        if (object instanceof ServiceInfo service) {
            return (I) resolveInternal(service, memo);
        }
        if (object instanceof Patch patch) {
            return (I) resolveInternal(patch);
        }
        return object;
    }

    /**
     * Resolves a {@link Patch} using internal utilities.
     *
     * @param patch The {@link Patch} to resolve; must not be null.
     * @return The resolved {@link Patch}.
     */
    private Patch resolveInternal(Patch patch) {
        Patch resolved = new Patch();
        for (Patch.Property p : patch.getPatches()) {
            Object resolvedValue = proxyUtils.resolvePatchPropertyValue(p.getValue());
            checkNotFounds(resolvedValue);
            resolved.add(p.getName(), resolvedValue);
        }
        return resolved;
    }

    private void checkNotFounds(Object value) {
        if (value instanceof Info info) {
            final ResolvingProxy resolvingProxy = getResolvingProxy(info);
            if (resolvingProxy != null) {
                onNotFound.accept(info, resolvingProxy);
            }
        } else if (value instanceof Iterable<?> it) {
            for (Object v : it) {
                checkNotFounds(v);
            }
        } else if (value instanceof Map<?, ?> map) {
            checkNotFounds(map.values());
        }
    }

    /**
     * Performs the actual proxy resolution using {@link ProxyUtils}.
     *
     * @param <I> The type of {@link Info}.
     * @param orig The proxy object to resolve; must not be null.
     * @return The resolved object, or null if not found.
     */
    protected <I extends Info> I doResolveProxy(final I orig) {
        return proxyUtils.resolve(orig);
    }

    /**
     * Checks if an object is a {@link ResolvingProxy}.
     *
     * @param unresolved The {@link Info} to check; may be null.
     * @return {@code true} if it’s a {@link ResolvingProxy}, {@code false} otherwise.
     */
    protected boolean isResolvingProxy(final Info unresolved) {
        return getResolvingProxy(unresolved) != null;
    }

    /**
     * Whether a reference held by an object must be resolved. A proxy must. With lookups in a facade, any object must
     * too, because it may hold proxies.
     */
    private boolean needsResolving(final Info reference) {
        if (reference == null) {
            return false;
        }
        return throughFacade || isResolvingProxy(reference);
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
     * Resolves a reference held by an object if {@link #needsResolving(Info)} says so, and returns it as it is
     * otherwise.
     */
    private <I extends Info> I resolveReference(I reference, CallMemo memo) {
        if (needsResolving(reference)) {
            return resolve(reference, memo);
        }
        return reference;
    }

    /**
     * Writes a resolved reference back to its object, unless it is the instance held by the object. Only a proxy
     * resolves to another instance.
     */
    private static <I> void replace(I held, I resolved, Consumer<I> setter) {
        if (resolved != held) {
            setter.accept(resolved);
        }
    }

    /** Resolves nested references in a {@link PublishedInfo} polymorphically. */
    @SuppressWarnings("unchecked")
    private <P extends PublishedInfo> P resolveInternal(P published, CallMemo memo) {
        if (published instanceof LayerInfo layer) {
            return (P) resolveInternal(layer, memo);
        }

        if (published instanceof LayerGroupInfo lg) {
            return (P) resolveInternal(lg, memo);
        }

        return published;
    }

    /** Resolves nested references in a {@link LayerInfo}. */
    private LayerInfo resolveInternal(LayerInfo layer, CallMemo memo) {
        ResourceInfo resource = layer.getResource();
        replace(resource, resolveReference(resource, memo), layer::setResource);

        StyleInfo defaultStyle = layer.getDefaultStyle();
        replace(defaultStyle, resolveReference(defaultStyle, memo), layer::setDefaultStyle);

        resolveStyles(layer, memo);
        return layer;
    }

    /**
     * Resolves the styles of a layer. A set holding a proxy is refilled with the resolved styles. Otherwise the styles
     * are resolved where they are: through a facade, a style that is not a proxy may still hold one, such as its
     * workspace. Either way, a set missing one of its own styles is refilled, see {@link #refillIfStale(Set)}.
     */
    private void resolveStyles(LayerInfo layer, CallMemo memo) {
        Set<StyleInfo> styles = layer.getStyles();
        if (styles.stream().anyMatch(this::isResolvingProxy)) {
            replaceStyles(styles, memo);
        } else {
            styles.forEach(style -> resolveReference(style, memo));
        }
        refillIfStale(styles);
    }

    /**
     * Refills a set of styles if it misses one of its own styles. The workspace of a style is part of its hash:
     * resolving the workspace of a style in place changes its hash, and the set keeps the style in the bucket of its
     * former hash. A listing row can give the same style instance to the default style of a layer and to its styles
     * set, and the default style is resolved first.
     */
    private static void refillIfStale(Set<StyleInfo> styles) {
        boolean stale = !styles.stream().allMatch(styles::contains);
        if (stale) {
            List<StyleInfo> copy = new ArrayList<>(styles);
            styles.clear();
            styles.addAll(copy);
        }
    }

    /** Refills the styles with their resolved instances, unless they all stay the same, as proxies not found do. */
    private void replaceStyles(Set<StyleInfo> styles, CallMemo memo) {
        List<StyleInfo> resolved = new ArrayList<>(styles.size());
        boolean changed = false;
        for (StyleInfo style : styles) {
            StyleInfo resolvedStyle = resolve(style, memo);
            if (resolvedStyle != style) {
                changed = true;
            }
            resolved.add(resolvedStyle);
        }
        if (changed) {
            styles.clear();
            styles.addAll(resolved);
        }
    }

    /** Resolves nested references in a {@link LayerGroupInfo}. */
    private LayerGroupInfo resolveInternal(LayerGroupInfo lg, CallMemo memo) {
        resolveMembers(lg, memo);
        resolveGroupStyles(lg, memo);

        WorkspaceInfo workspace = lg.getWorkspace();
        replace(workspace, resolve(workspace, memo), lg::setWorkspace);

        LayerInfo rootLayer = lg.getRootLayer();
        replace(rootLayer, resolve(rootLayer, memo), lg::setRootLayer);

        StyleInfo rootLayerStyle = lg.getRootLayerStyle();
        replace(rootLayerStyle, resolve(rootLayerStyle, memo), lg::setRootLayerStyle);
        return lg;
    }

    /**
     * Resolves the members of a group, and through a facade the layers of its layer group styles. One call loads the
     * members not resolved yet by this call. A member found is resolved in turn: a nested group loads its own members
     * with one more call.
     */
    private void resolveMembers(LayerGroupInfo lg, CallMemo memo) {
        List<List<PublishedInfo>> memberLists = memberLists(lg);
        Map<String, PublishedInfo> found = loadNotResolved(memberLists, PublishedInfo.class, memo);
        for (List<PublishedInfo> members : memberLists) {
            replaceEntries(members, member -> resolveEntry(found, member, memo));
        }
    }

    /** Resolves the styles of a group, and through a facade the styles of its layer group styles, like the members. */
    private void resolveGroupStyles(LayerGroupInfo lg, CallMemo memo) {
        List<List<StyleInfo>> styleLists = styleLists(lg);
        Map<String, StyleInfo> found = loadNotResolved(styleLists, StyleInfo.class, memo);
        for (List<StyleInfo> styles : styleLists) {
            replaceEntries(styles, style -> resolveStyleEntry(found, style, memo));
        }
    }

    private List<List<PublishedInfo>> memberLists(LayerGroupInfo lg) {
        List<List<PublishedInfo>> lists = new ArrayList<>();
        lists.add(lg.getLayers());
        for (LayerGroupStyle groupStyle : layerGroupStylesToResolve(lg)) {
            if (groupStyle.getLayers() != null) {
                lists.add(groupStyle.getLayers());
            }
        }
        return lists;
    }

    private List<List<StyleInfo>> styleLists(LayerGroupInfo lg) {
        List<List<StyleInfo>> lists = new ArrayList<>();
        lists.add(lg.getStyles());
        for (LayerGroupStyle groupStyle : layerGroupStylesToResolve(lg)) {
            if (groupStyle.getStyles() != null) {
                lists.add(groupStyle.getStyles());
            }
        }
        return lists;
    }

    /** The layer group styles of a group, resolved through a facade only, see {@link #throughFacade}. */
    private List<LayerGroupStyle> layerGroupStylesToResolve(LayerGroupInfo lg) {
        if (!throughFacade || lg.getLayerGroupStyles() == null) {
            return List.of();
        }
        return lg.getLayerGroupStyles();
    }

    /**
     * Loads the objects referenced by the proxies of the lists with one call, keyed by id. The references known to the
     * call are left out. The objects loaded enter the memo at once, before their references are resolved: a nested
     * group then finds the members shared with its parent in the memo.
     */
    private <I extends CatalogInfo> Map<String, I> loadNotResolved(List<List<I>> lists, Class<I> type, CallMemo memo) {
        List<I> unknown = unknownReferences(lists, memo);
        Map<String, I> found = proxyUtils.loadAllById(ProxyUtils.referencedIds(unknown), type);
        rememberFound(unknown, found, memo);
        return found;
    }

    /** The proxies of the lists holding a reference not looked up yet by the call. */
    private <I extends Info> List<I> unknownReferences(List<List<I>> lists, CallMemo memo) {
        List<I> unknown = new ArrayList<>();
        for (List<I> references : lists) {
            for (I reference : references) {
                ResolvingProxy handler = getResolvingProxy(reference);
                if (handler != null && !memo.knows(Reference.of(reference, handler))) {
                    unknown.add(reference);
                }
            }
        }
        return unknown;
    }

    /** Puts the objects of a bulk call in the memo, under the references of the proxies pointing to them. */
    private <I extends Info> void rememberFound(List<I> proxies, Map<String, I> found, CallMemo memo) {
        for (I proxy : proxies) {
            I object = ProxyUtils.findReferenced(found, proxy);
            if (object != null) {
                memo.rememberFound(Reference.of(proxy, getResolvingProxy(proxy)), object);
            }
        }
    }

    /** Replaces the entries of a list resolving to another instance, in place. */
    private static <I> void replaceEntries(List<I> entries, UnaryOperator<I> resolution) {
        for (int i = 0; i < entries.size(); i++) {
            I entry = entries.get(i);
            I resolved = resolution.apply(entry);
            if (resolved != entry) {
                entries.set(i, resolved);
            }
        }
    }

    /** Takes the entry from the bulk lookup results, or resolves it on its own when they do not hold it. */
    private <I extends Info> I resolveEntry(Map<String, I> found, I entry, CallMemo memo) {
        I bulkResult = ProxyUtils.findReferenced(found, entry);
        if (bulkResult == null) {
            return resolve(entry, memo);
        }
        return resolveFound(bulkResult, memo);
    }

    /** Resolves a style entry like {@link #resolveEntry}, except that a proxy with an empty reference becomes null. */
    private StyleInfo resolveStyleEntry(Map<String, StyleInfo> styles, StyleInfo s, CallMemo memo) {
        ResolvingProxy proxy = getResolvingProxy(s);
        if (proxy != null && !StringUtils.hasLength(proxy.getRef())) {
            return null;
        }
        return resolveEntry(styles, s, memo);
    }

    /** Resolves nested references in a {@link StyleInfo}. */
    private StyleInfo resolveInternal(StyleInfo style, CallMemo memo) {
        WorkspaceInfo workspace = style.getWorkspace();
        replace(workspace, resolveReference(workspace, memo), style::setWorkspace);
        return style;
    }

    /** Resolves nested references in a {@link StoreInfo}. */
    private StoreInfo resolveInternal(StoreInfo store, CallMemo memo) {
        WorkspaceInfo workspace = store.getWorkspace();
        replace(workspace, resolveReference(workspace, memo), store::setWorkspace);
        return store;
    }

    /** Resolves nested references in a {@link SettingsInfo}. */
    private SettingsInfo resolveInternal(SettingsInfo settings, CallMemo memo) {
        WorkspaceInfo workspace = settings.getWorkspace();
        replace(workspace, resolve(workspace, memo), settings::setWorkspace);
        return settings;
    }

    /** Resolves nested references in a {@link ServiceInfo}. */
    private ServiceInfo resolveInternal(ServiceInfo service, CallMemo memo) {
        WorkspaceInfo workspace = service.getWorkspace();
        replace(workspace, resolve(workspace, memo), service::setWorkspace);
        return service;
    }

    /** Resolves nested references in a {@link ResourceInfo}. */
    private ResourceInfo resolveInternal(ResourceInfo resource, CallMemo memo) {
        StoreInfo store = resource.getStore();
        replace(store, resolveReference(store, memo), resource::setStore);

        NamespaceInfo namespace = resource.getNamespace();
        replace(namespace, resolveReference(namespace, memo), resource::setNamespace);
        return resource;
    }

    /** A memoizing variant of {@link ResolvingProxyResolver} that caches resolved objects. */
    private static class MemoizingProxyResolver extends ResolvingProxyResolver<Info> {

        private Map<String, Info> resolvedById = new ConcurrentHashMap<>();

        /**
         * Constructs a memoizing resolver with the lookups and the not-found behavior of another resolver.
         *
         * @param proxyUtils The lookups of the other resolver; must not be null.
         * @param throughFacade Whether the lookups go to a facade instead of the catalog.
         * @param onNotFound The action for unresolved proxies; must not be null.
         */
        MemoizingProxyResolver(
                ProxyUtils proxyUtils, boolean throughFacade, BiConsumer<Info, ResolvingProxy> onNotFound) {
            super(proxyUtils, throughFacade, onNotFound);
        }

        /**
         * Resolves a proxy, using the cache to avoid repeated lookups.
         *
         * @param <I> The type of {@link Info}.
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
         * @param <I> The type of {@link Info}.
         * @param orig The proxy object to resolve; must not be null.
         * @return The resolved object.
         */
        @SuppressWarnings("unchecked")
        private <I extends Info> I computeIfAbsent(final I orig) {
            return (I) resolvedById.computeIfAbsent(orig.getId(), key -> super.doResolveProxy(orig));
        }
    }

    /**
     * The objects resolved during one call. The objects of a call share references, such as the store of the layers of
     * a group. With the memo of a call through a facade, a reference is looked up once, found or not, and the
     * references of an object are resolved once. The memo lives only as long as the call: it is not a cache, and it
     * cannot serve an object made stale by a later change.
     */
    private interface CallMemo {

        /** The memo of the catalog path, see {@link NoMemo}. */
        CallMemo NONE = new NoMemo();

        @Nullable
        Info found(Reference reference);

        boolean isNotFound(Reference reference);

        /** Whether the call looked the reference up already, finding its object or not. */
        boolean knows(Reference reference);

        void rememberFound(Reference reference, Info object);

        void rememberNotFound(Reference reference);

        /**
         * Whether the call meets an object for the first time. Objects are told apart by identity: two copies of one
         * catalog object hold proxies of their own.
         */
        boolean firstVisit(Info object);
    }

    /** The memo of one call through a facade. */
    private static final class FacadeCallMemo implements CallMemo {

        private final Map<Reference, Info> found = new HashMap<>();
        private final Set<Reference> notFound = new HashSet<>();
        private final Set<Info> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        @Override
        public @Nullable Info found(Reference reference) {
            return found.get(reference);
        }

        @Override
        public boolean isNotFound(Reference reference) {
            return notFound.contains(reference);
        }

        @Override
        public boolean knows(Reference reference) {
            return found.containsKey(reference) || notFound.contains(reference);
        }

        @Override
        public void rememberFound(Reference reference, Info object) {
            found.put(reference, object);
        }

        @Override
        public void rememberNotFound(Reference reference) {
            notFound.add(reference);
        }

        @Override
        public boolean firstVisit(Info object) {
            return visited.add(object);
        }
    }

    /**
     * The memo of the catalog path. It remembers nothing: a reference is looked up where it is met, and each object is
     * visited. It holds no state, and one instance serves the calls of all threads.
     */
    private static final class NoMemo implements CallMemo {

        @Override
        public @Nullable Info found(Reference reference) {
            return null;
        }

        @Override
        public boolean isNotFound(Reference reference) {
            return false;
        }

        @Override
        public boolean knows(Reference reference) {
            return false;
        }

        @Override
        public void rememberFound(Reference reference, Info object) {
            // remembers nothing
        }

        @Override
        public void rememberNotFound(Reference reference) {
            // remembers nothing
        }

        @Override
        public boolean firstVisit(Info object) {
            return true;
        }
    }

    /**
     * The reference held by a proxy: the class of the proxy, made by the JDK for the referenced interface such as
     * {@code DataStoreInfo}, and the id or name with the prefix of a name.
     */
    private record Reference(Class<?> proxyClass, @Nullable String ref, @Nullable String prefix) {

        static Reference of(Info proxy, ResolvingProxy handler) {
            return new Reference(proxy.getClass(), handler.getRef(), handler.getPrefix());
        }
    }
}
