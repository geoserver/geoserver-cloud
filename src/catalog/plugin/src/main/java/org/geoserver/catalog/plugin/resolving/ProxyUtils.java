/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import lombok.Getter;
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

import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** */
@Slf4j
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
    private static final Set<Class<? extends Info>> VALUE_BY_REFERENCE_TYPES =
            Set.of(
                    WorkspaceInfo.class, //
                    NamespaceInfo.class, //
                    StoreInfo.class, //
                    ResourceInfo.class, //
                    PublishedInfo.class, //
                    MapInfo.class, //
                    StyleInfo.class, //
                    GeoServerInfo.class, //
                    ServiceInfo.class);

    private final @NonNull @Getter Catalog catalog;
    private final @NonNull @Getter Optional<GeoServer> config;

    private boolean failOnNotFound = false;

    /**
     * @param fail if {@code true}, a proxied {@link Info} reference that's not found in the catalog
     *     will result in an {@link IllegalStateException}
     * @return {@code this}
     */
    public ProxyUtils failOnMissingReference(boolean fail) {
        this.failOnNotFound = fail;
        return this;
    }

    private <T> T unwrap(T obj) {
        return ModificationProxy.unwrap(obj);
    }

    public Patch resolve(Patch patch) {
        Patch resolved = new Patch();
        for (Patch.Property p : patch.getPatches()) {
            resolved.add(p.getName(), resolvePatchPropertyValue(p.getValue()));
        }
        return resolved;
    }

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
            resolve(list);
            return list;
        }
        if (orig instanceof Set) {
            @SuppressWarnings("unchecked")
            Set<Object> set = (Set<Object>) orig;
            return resolve(set);
        }
        return orig;
    }

    private AttributeTypeInfo resolve(AttributeTypeInfo orig) {
        FeatureTypeInfo ft = orig.getFeatureType();
        FeatureTypeInfo resolvedFt = resolve(ft);
        orig.setFeatureType(resolvedFt);
        return orig;
    }

    private void resolve(List<Object> mutableList) {
        for (int i = 0; i < mutableList.size(); i++) {
            Object v = mutableList.get(i);
            Object resolved = resolvePatchPropertyValue(v);
            mutableList.set(i, resolved);
        }
    }

    private Set<Object> resolve(Set<Object> set) {
        Set<Object> target = newSet(set.getClass());
        for (Object value : set) {
            target.add(resolvePatchPropertyValue(value));
        }
        return target;
    }

    @SuppressWarnings("unchecked")
    private Set<Object> newSet(@SuppressWarnings("rawtypes") Class<? extends Set> class1) {
        try {
            return class1.getConstructor().newInstance();
        } catch (Exception e) {
            return new HashSet<Object>();
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Info> T resolve(final T unresolved) {
        if (unresolved == null) {
            return null;
        }
        boolean isResolvingProxy = isResolvingProxy(unresolved);
        T info = unwrap(unresolved);
        if (isResolvingProxy) {
            if (info instanceof CatalogInfo) info = resolve(catalog, info);
            else if (info instanceof GeoServerInfo && this.config.isPresent())
                info = (T) this.config.get().getGlobal();
            else if (info instanceof LoggingInfo && this.config.isPresent())
                info = (T) this.config.get().getLogging();
            else if (info instanceof ServiceInfo && this.config.isPresent())
                info = (T) this.config.get().getService(info.getId(), ServiceInfo.class);
        }

        if (info == null) {
            if (failOnNotFound)
                throw new IllegalArgumentException("Reference to " + unresolved.getId());
            return null;
        } else if (!Proxy.isProxyClass(info.getClass())) {
            if (info instanceof StyleInfo s) resolveInternal(s);
            if (info instanceof LayerInfo l) resolveInternal(l);
            if (info instanceof LayerGroupInfo lg) resolveInternal(lg);
            if (info instanceof ResourceInfo r) resolveInternal(r);
            if (info instanceof StoreInfo s) resolveInternal(s);
            if (info instanceof SettingsInfo s) resolveInternal(s);
            if (info instanceof ServiceInfo s) resolveInternal(s);
            if (info instanceof GeoServerInfo g) resolveInternal(g);
            if (info instanceof LoggingInfo l) resolveInternal(l);
        }
        return info;
    }

    @SuppressWarnings("unchecked")
    private <T extends Info> T resolve(@NonNull Catalog catalog, T info) {
        // Workaround for a bug in ResolvingProxy.resolve(), if info is a PublishedInfo (as opposed
        // to a concrete LayerInfo or LayerGroupInfo) it does nothing.
        if (info instanceof PublishedInfo) {
            PublishedInfo l =
                    ResolvingProxy.resolve(
                            catalog, ResolvingProxy.create(info.getId(), LayerInfo.class));
            if (null == l) {
                l =
                        ResolvingProxy.resolve(
                                catalog, ResolvingProxy.create(info.getId(), LayerGroupInfo.class));
            }
            return (T) l;
        }
        return ResolvingProxy.resolve(catalog, info);
    }

    public static <T extends Info> boolean isResolvingProxy(final T info) {
        return null != org.geoserver.catalog.impl.ProxyUtils.handler(info, ResolvingProxy.class);
    }

    public static <T extends Info> boolean isModificationProxy(final T info) {
        return null != org.geoserver.catalog.impl.ProxyUtils.handler(info, ModificationProxy.class);
    }

    private void resolveInternal(LoggingInfo info) {}

    private void resolveInternal(GeoServerInfo info) {}

    protected void resolveInternal(SettingsInfo settings) {
        if (settings.getWorkspace() != null) {
            settings.setWorkspace(resolve(settings.getWorkspace()));
        }
    }

    protected void resolveInternal(ServiceInfo service) {
        if (service.getWorkspace() != null) {
            service.setWorkspace(resolve(service.getWorkspace()));
        }
    }

    protected void resolveInternal(LayerInfo layer) {
        layer.setResource(resolve(layer.getResource()));
        layer.setDefaultStyle(resolve(layer.getDefaultStyle()));
        LinkedHashSet<StyleInfo> styles = new LinkedHashSet<StyleInfo>();
        for (StyleInfo s : layer.getStyles()) {
            styles.add(resolve(s));
        }
        ((LayerInfoImpl) layer).setStyles(styles);
    }

    protected <T extends PublishedInfo> T resolveInternal(T published) {
        if (published instanceof LayerInfo l) resolve(l);
        else if (published instanceof LayerGroupInfo lg) resolve(lg);
        return published;
    }

    protected void resolveInternal(LayerGroupInfo layerGroup) {
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
    }

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

    public static Optional<Class<? extends Info>> referenceTypeOf(Object val) {
        return Optional.ofNullable(val)
                .filter(Info.class::isInstance)
                .flatMap(
                        i ->
                                VALUE_BY_REFERENCE_TYPES.stream()
                                        .filter(type -> type.isInstance(i))
                                        .findFirst());
    }

    public static boolean encodeByReference(Object o) {
        return referenceTypeOf(o).isPresent();
    }
}
