/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.resolving;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.impl.ResourceInfoImpl;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

/**
 * A utility class for resolving {@link Info} objects and {@link Patch} properties within a GeoServer
 * catalog context, handling proxy references and nested structures.
 *
 * <p>This class provides methods to resolve {@link CatalogInfo} and configuration {@link Info} objects by
 * replacing {@link ResolvingProxy} references with actual catalog instances, managing nested collections,
 * and determining whether objects should be encoded as references in a {@link Patch}. It uses a catalog
 * supplier and an optional {@link GeoServer} configuration to fetch resolved instances, supporting both
 * catalog entities (e.g., {@link StoreInfo}) and configuration objects (e.g., {@link ServiceInfo}).
 *
 * <p>Key features:
 * <ul>
 *   <li><strong>Proxy Resolution:</strong> Resolves {@link ResolvingProxy} instances to actual objects
 *       using the provided catalog and configuration.</li>
 *   <li><strong>Patch Processing:</strong> Transforms {@link Patch} properties, resolving nested
 *       {@link Info} objects and collections.</li>
 *   <li><strong>Reference Detection:</strong> Identifies types (e.g., {@link WorkspaceInfo}) to encode as
 *       references rather than full values in patches.</li>
 *   <li><strong>Configurability:</strong> Allows toggling failure on missing references via
 *       {@link #failOnMissingReference(boolean)}.</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * Catalog catalog = ...;
 * ProxyUtils utils = new ProxyUtils(() -> catalog, Optional.empty());
 * LayerInfo layer = ...; // contains ResolvingProxy references
 * LayerInfo resolved = utils.resolve(layer);
 * </pre>
 *
 * @since 1.0
 * @see ResolvingProxy
 * @see ModificationProxy
 * @see Patch
 */
@Slf4j(topic = "org.geoserver.catalog.plugin.resolving")
@RequiredArgsConstructor
public class ProxyUtils {
    /**
     * Types that shall be encoded as {@link InfoReference reference} instead of as a full value
     * object when coming as a Patch {@link Patch.Property#getValue() Property} value. Some config
     * info types like {@link SettingsInfo} and {@link LoggingInfo} shall be encoded as values, and
     * so all other {@link Info} subtypes that are not entities but value objects (such as {@link
     * AttributionInfo}, {@link LegendInfo}, etc.)
     *
     * @see #encodeByReference
     * @see #referenceTypeOf
     */
    private static final Set<Class<? extends Info>> VALUE_BY_REFERENCE_TYPES = Set.of(
            WorkspaceInfo.class, //
            NamespaceInfo.class, //
            StoreInfo.class, //
            ResourceInfo.class, //
            PublishedInfo.class, //
            MapInfo.class, //
            StyleInfo.class, //
            GeoServerInfo.class, //
            ServiceInfo.class);

    private final @NonNull Supplier<Catalog> catalog;
    private final @NonNull Optional<GeoServer> config;

    private boolean failOnNotFound = false;

    /**
     * Configures whether unresolved proxy references should throw an exception.
     *
     * <p>If set to {@code true}, a {@link ResolvingProxy} reference not found in the catalog or
     * configuration will result in an {@link IllegalStateException}. If {@code false} (default), such
     * cases return null without failing.
     *
     * @param fail If {@code true}, fail on unresolved references; if {@code false}, return null.
     * @return This {@link ProxyUtils} instance for chaining.
     */
    public ProxyUtils failOnMissingReference(boolean fail) {
        this.failOnNotFound = fail;
        return this;
    }

    /**
     * Resolves all {@link Info} references within a {@link Patch}.
     *
     * <p>Creates a new {@link Patch} where each property value is processed by
     * {@link #resolvePatchPropertyValue(Object)}, handling {@link Info} objects, collections, and nested
     * structures.
     *
     * @param patch The {@link Patch} to resolve; must not be null.
     * @return A new {@link Patch} with resolved property values.
     * @throws NullPointerException if {@code patch} is null.
     */
    public Patch resolve(Patch patch) {
        Patch resolved = new Patch();
        for (Patch.Property p : patch.getPatches()) {
            resolved.add(p.getName(), resolvePatchPropertyValue(p.getValue()));
        }
        return resolved;
    }

    /**
     * Resolves a single patch property value, handling various types.
     *
     * <p>Processes the value based on its type: resolves {@link Info} objects via {@link #resolve(Info)},
     * {@link AttributeTypeInfo} via {@link #resolve(AttributeTypeInfo)}, and collections ({@link List},
     * {@link Set}) via their respective methods. Non-matching types are returned unchanged.
     *
     * @param orig The value to resolve; may be null.
     * @return The resolved value, or {@code orig} if unchanged.
     */
    private Object resolvePatchPropertyValue(Object orig) {
        if (orig instanceof Info info) {
            return resolve(info);
        }
        if (orig instanceof AttributeTypeInfo att) {
            return resolve(att);
        }
        if (orig instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) orig;
            return resolve(list);
        }
        if (orig instanceof Set) {
            @SuppressWarnings("unchecked")
            Set<Object> set = (Set<Object>) orig;
            return resolve(set);
        }
        return orig;
    }

    /**
     * Resolves an {@link AttributeTypeInfo} by updating its feature type reference.
     *
     * <p>Replaces the feature type with its resolved version using {@link #resolve(Info)}.
     *
     * @param orig The {@link AttributeTypeInfo} to resolve; must not be null.
     * @return The resolved {@link AttributeTypeInfo}.
     * @throws NullPointerException if {@code orig} is null.
     */
    private AttributeTypeInfo resolve(AttributeTypeInfo orig) {
        FeatureTypeInfo ft = orig.getFeatureType();
        FeatureTypeInfo resolvedFt = resolve(ft);
        orig.setFeatureType(resolvedFt);
        return orig;
    }

    /**
     * Resolves a list of objects by processing each element.
     *
     * <p>Creates a new {@link ArrayList} with each element resolved via
     * {@link #resolvePatchPropertyValue(Object)}.
     *
     * @param mutableList The list to resolve; must not be null.
     * @return A new list with resolved elements.
     * @throws NullPointerException if {@code mutableList} is null.
     */
    private List<Object> resolve(List<Object> mutableList) {
        return mutableList.stream()
                .map(this::resolvePatchPropertyValue)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Resolves a set of objects by processing each element.
     *
     * <p>Creates a new set of the same type (or {@link HashSet} if instantiation fails) with each element
     * resolved via {@link #resolvePatchPropertyValue(Object)}.
     *
     * @param set The set to resolve; must not be null.
     * @return A new set with resolved elements.
     * @throws NullPointerException if {@code set} is null.
     */
    private Set<Object> resolve(Set<Object> set) {
        Set<Object> target = newSet(set.getClass());
        for (Object value : set) {
            target.add(resolvePatchPropertyValue(value));
        }
        return target;
    }

    /**
     * Creates a new set instance of the specified type, falling back to {@link HashSet} on failure.
     *
     * @param class1 The set class to instantiate; must not be null.
     * @return A new empty set instance.
     */
    @SuppressWarnings("unchecked")
    private Set<Object> newSet(@SuppressWarnings("rawtypes") Class<? extends Set> class1) {
        try {
            return class1.getConstructor().newInstance();
        } catch (Exception e) {
            return new HashSet<>();
        }
    }

    /**
     * Resolves an {@link Info} object, handling proxies and nested references.
     *
     * <p>Unwraps any {@link ModificationProxy}, resolves {@link ResolvingProxy} references using the
     * catalog or configuration, and processes nested references via type-specific methods. Returns null if
     * unresolved and {@link #failOnMissingReference(boolean)} is false; otherwise, throws an exception.
     *
     * @param <T>        The type of {@link Info}.
     * @param unresolved The {@link Info} object to resolve; may be null.
     * @return The resolved {@link Info}, or null if unresolved and not failing.
     * @throws IllegalArgumentException if an unresolved proxy is encountered and {@code failOnNotFound} is true.
     */
    @SuppressWarnings("unchecked")
    public <T extends Info> T resolve(final T unresolved) {
        if (unresolved == null) {
            return null;
        }
        T info = ModificationProxy.unwrap(unresolved);
        if (isResolvingProxy(unresolved)) {
            info = resolveResolvingProxy(info);
        }

        if (info == null) {
            if (failOnNotFound) {
                throw new IllegalArgumentException("Reference to %s not found".formatted(unresolved.getId()));
            }
            return null;
        }

        if (!Proxy.isProxyClass(info.getClass())) {
            info = (T) resolveInternal(info);
        }
        return info;
    }

    /**
     * Dispatches resolution to type-specific internal methods.
     *
     * @param info The {@link Info} object to resolve; must not be null.
     * @return The resolved {@link Info}.
     */
    private Info resolveInternal(Info info) {
        if (info instanceof StyleInfo s) {
            return resolveInternal(s);
        }
        if (info instanceof LayerInfo l) {
            return resolveInternal(l);
        }
        if (info instanceof LayerGroupInfo lg) {
            return resolveInternal(lg);
        }
        if (info instanceof ResourceInfo r) {
            return resolveInternal(r);
        }
        if (info instanceof StoreInfo s) {
            return resolveInternal(s);
        }
        if (info instanceof SettingsInfo s) {
            return resolveInternal(s);
        }
        if (info instanceof ServiceInfo s) {
            return resolveInternal(s);
        }
        return info;
    }

    /**
     * Resolves a {@link ResolvingProxy} reference to a catalog or configuration object.
     *
     * <p>For {@link CatalogInfo}, uses the catalog; for configuration objects (e.g., {@link GeoServerInfo}),
     * uses the optional {@link GeoServer} instance.
     *
     * @param <T>  The type of {@link Info}.
     * @param info The {@link Info} proxy to resolve; must not be null.
     * @return The resolved {@link Info}, or the original if unresolved.
     */
    @SuppressWarnings("unchecked")
    private <T extends Info> T resolveResolvingProxy(T info) {
        if (info instanceof CatalogInfo) {
            return resolveCatalogInfo(info);
        }

        GeoServer gsConfig = this.config.orElse(null);
        if (gsConfig != null) {
            if (info instanceof GeoServerInfo) {
                return (T) gsConfig.getGlobal();
            }

            if (info instanceof LoggingInfo) {
                return (T) gsConfig.getLogging();
            }

            if (info instanceof ServiceInfo) {
                String serviceId = info.getId();
                return (T) gsConfig.getService(serviceId, ServiceInfo.class);
            }
        }
        return info;
    }

    /**
     * Resolves a {@link CatalogInfo} proxy using the catalog.
     *
     * <p>Handles special case for {@link PublishedInfo} to try both {@link LayerInfo} and
     * {@link LayerGroupInfo} resolutions due to a known {@link ResolvingProxy} limitation.
     *
     * @param <T>  The type of {@link CatalogInfo}.
     * @param info The {@link CatalogInfo} proxy to resolve; must not be null.
     * @return The resolved {@link CatalogInfo}.
     */
    @SuppressWarnings("unchecked")
    private <T extends Info> T resolveCatalogInfo(T info) {
        Catalog actualCatalog = getCatalog();
        // Workaround for a bug in ResolvingProxy.resolve(), if info is a PublishedInfo
        // (as opposed
        // to a concrete LayerInfo or LayerGroupInfo) it does nothing.
        if (info instanceof PublishedInfo) {
            PublishedInfo l =
                    ResolvingProxy.resolve(actualCatalog, ResolvingProxy.create(info.getId(), LayerInfo.class));
            if (null == l) {
                l = ResolvingProxy.resolve(actualCatalog, ResolvingProxy.create(info.getId(), LayerGroupInfo.class));
            }
            return (T) l;
        }
        return ResolvingProxy.resolve(actualCatalog, info);
    }

    /**
     * Retrieves the current catalog instance from the supplier.
     *
     * @return The {@link Catalog}; never null.
     */
    @NonNull
    private Catalog getCatalog() {
        return catalog.get();
    }

    /**
     * Checks if an {@link Info} object is a {@link ResolvingProxy}.
     *
     * @param <T>  The type of {@link Info}.
     * @param info The object to check; may be null.
     * @return {@code true} if it’s a {@link ResolvingProxy}, {@code false} otherwise.
     */
    public static <T extends Info> boolean isResolvingProxy(final T info) {
        return null != org.geoserver.catalog.impl.ProxyUtils.handler(info, ResolvingProxy.class);
    }

    /**
     * Checks if an {@link Info} object is a {@link ModificationProxy}.
     *
     * @param <T>  The type of {@link Info}.
     * @param info The object to check; may be null.
     * @return {@code true} if it’s a {@link ModificationProxy}, {@code false} otherwise.
     */
    public static <T extends Info> boolean isModificationProxy(final T info) {
        return null != org.geoserver.catalog.impl.ProxyUtils.handler(info, ModificationProxy.class);
    }

    /**
     * Resolves nested references in a {@link SettingsInfo} object.
     *
     * @param settings The {@link SettingsInfo} to resolve; must not be null.
     * @return The resolved {@link SettingsInfo}.
     */
    protected SettingsInfo resolveInternal(SettingsInfo settings) {
        if (settings.getWorkspace() != null) {
            settings.setWorkspace(resolve(settings.getWorkspace()));
        }
        return settings;
    }

    /**
     * Resolves nested references in a {@link ServiceInfo} object.
     *
     * @param service The {@link ServiceInfo} to resolve; must not be null.
     * @return The resolved {@link ServiceInfo}.
     */
    protected ServiceInfo resolveInternal(ServiceInfo service) {
        if (service.getWorkspace() != null) {
            service.setWorkspace(resolve(service.getWorkspace()));
        }
        return service;
    }

    /**
     * Resolves nested references in a {@link LayerInfo} object.
     *
     * @param layer The {@link LayerInfo} to resolve; must not be null.
     * @return The resolved {@link LayerInfo}.
     */
    protected LayerInfo resolveInternal(LayerInfo layer) {
        layer.setResource(resolve(layer.getResource()));
        layer.setDefaultStyle(resolve(layer.getDefaultStyle()));
        LinkedHashSet<StyleInfo> styles = new LinkedHashSet<>();
        for (StyleInfo s : layer.getStyles()) {
            styles.add(resolve(s));
        }
        ((LayerInfoImpl) layer).setStyles(styles);
        return layer;
    }

    /**
     * Resolves nested references in a {@link PublishedInfo} object polymorphically.
     *
     * @param <T>       The type of {@link PublishedInfo}.
     * @param published The {@link PublishedInfo} to resolve; must not be null.
     * @return The resolved {@link PublishedInfo}.
     */
    protected <T extends PublishedInfo> T resolveInternal(T published) {
        if (published instanceof LayerInfo l) {
            resolve(l);
        } else if (published instanceof LayerGroupInfo lg) {
            resolve(lg);
        }
        return published;
    }

    /**
     * Resolves nested references in a {@link LayerGroupInfo} object.
     *
     * @param layerGroup The {@link LayerGroupInfo} to resolve; must not be null.
     * @return The resolved {@link LayerGroupInfo}.
     */
    protected LayerGroupInfo resolveInternal(LayerGroupInfo layerGroup) {
        LayerGroupInfoImpl lg = (LayerGroupInfoImpl) layerGroup;

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

    /**
     * Resolves nested references in a {@link StyleInfo} object.
     *
     * @param style The {@link StyleInfo} to resolve; must not be null.
     * @return The resolved {@link StyleInfo}.
     */
    protected StyleInfo resolveInternal(StyleInfo style) {
        // resolve the workspace
        WorkspaceInfo ws = style.getWorkspace();
        if (ws != null) {
            style.setWorkspace(resolve(ws));
            if (style.getWorkspace() == null) {
                log.info(
                        "Failed to resolve workspace for style \"{}\". This means the workspace has not yet been added to the catalog, keep the proxy around",
                        style.getName());
            }
        }
        return style;
    }

    /**
     * Resolves nested references in a {@link StoreInfo} object.
     *
     * @param store The {@link StoreInfo} to resolve; must not be null.
     * @return The resolved {@link StoreInfo}.
     */
    protected StoreInfo resolveInternal(StoreInfo store) {
        StoreInfoImpl s = (StoreInfoImpl) store;

        // resolve the workspace
        WorkspaceInfo ws = store.getWorkspace();
        if (ws != null) {
            s.setWorkspace(resolve(ws));
            if (store.getWorkspace() == null) {
                log.info(
                        "Failed to resolve workspace for store \"{}\". This means the workspace has not yet been added to the catalog, keep the proxy around",
                        store.getName());
            }
        }
        return s;
    }

    /**
     * Resolves nested references in a {@link ResourceInfo} object.
     *
     * @param resource The {@link ResourceInfo} to resolve; must not be null.
     * @return The resolved {@link ResourceInfo}.
     */
    protected ResourceInfo resolveInternal(ResourceInfo resource) {
        ResourceInfoImpl r = (ResourceInfoImpl) resource;

        // resolve the store
        StoreInfo store = resource.getStore();
        if (store != null) {
            r.setStore(resolve(store));
        }

        // resolve the namespace
        NamespaceInfo namespace = resource.getNamespace();
        if (namespace != null) {
            r.setNamespace(resolve(namespace));
        }
        return r;
    }

    /**
     * Determines if an object should be encoded as a reference in a {@link Patch}.
     *
     * @param val The object to check; may be null.
     * @return An {@link Optional} containing the {@link Info} type if it should be a reference, or empty if not.
     */
    public static Optional<Class<? extends Info>> referenceTypeOf(Object val) {
        return Optional.ofNullable(val).filter(Info.class::isInstance).flatMap(i -> VALUE_BY_REFERENCE_TYPES.stream()
                .filter(type -> type.isInstance(i))
                .findFirst());
    }

    /**
     * Checks if an object should be encoded as a reference rather than a full value in a {@link Patch}.
     *
     * @param o The object to check; may be null.
     * @return {@code true} if it should be encoded as a reference, {@code false} otherwise.
     * @see #referenceTypeOf
     */
    public static boolean encodeByReference(Object o) {
        return referenceTypeOf(o).isPresent();
    }
}
