/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.plugin.requestcache;

import static org.geoserver.catalog.plugin.requestcache.RequestCacheAccess.evict;
import static org.geoserver.catalog.plugin.requestcache.RequestCacheAccess.liveCache;
import static org.geoserver.catalog.plugin.requestcache.RequestCacheAccess.memoized;

import java.util.Optional;
import java.util.function.Supplier;
import lombok.NonNull;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.plugin.forwarding.ForwardingGeoServerFacade;
import org.jspecify.annotations.Nullable;

/**
 * Remembers the configuration lookups of a request: the global settings, the logging settings, the settings of the
 * workspaces and the services. The answers go to the {@link RequestCache} bound to the thread by
 * {@link RequestCacheScope}, the lookups finding nothing included. Outside a scope, and once the request ended, the
 * lookups run uncached. The listings of services are never cached.
 *
 * <p>The request sees its own writes: a write evicts the keys of the changed object. The configuration lookups share
 * the cache with the catalog lookups of {@link RequestCachingCatalogFacade}, and a change or removal of a catalog
 * object clears them too. A change made by another thread of this instance shows from the next request on. A change
 * made through another service instance shows at the next lookup, see {@link RequestCache}.
 *
 * <p>The facade below wraps its answers in a new {@link ModificationProxy} per call. The cache keeps the object without
 * its proxy, and each read wraps it in a new proxy: the getters of a proxy write to it, and the threads of a request
 * must not read one proxy at once. The callers of a request share the object, and see the changes once one of them
 * saves.
 */
public class RequestCachingGeoServerFacade extends ForwardingGeoServerFacade {

    private static final IdKey GLOBAL_KEY = new IdKey(ClassMappings.GLOBAL, "global");

    private static final IdKey LOGGING_KEY = new IdKey(ClassMappings.LOGGING, "logging");

    /** The prefix of the keys of the settings of a workspace, followed by the id of the workspace. */
    private static final String SETTINGS_PREFIX = "settings:";

    /** The prefix of the keys of the lookups of a service by class. */
    private static final String SERVICE_PREFIX = "service:";

    /** The prefix of the keys of the lookups of a service by name. */
    private static final String SERVICE_NAME_PREFIX = "service-name:";

    public RequestCachingGeoServerFacade(@NonNull GeoServerFacade facade) {
        super(facade);
    }

    @Override
    public GeoServerInfo getGlobal() {
        return memoizedProxy(GLOBAL_KEY, GeoServerInfo.class, super::getGlobal);
    }

    @Override
    public void setGlobal(GeoServerInfo global) {
        super.setGlobal(global);
        evict(GLOBAL_KEY);
    }

    @Override
    public void save(GeoServerInfo geoServer) {
        super.save(geoServer);
        evict(GLOBAL_KEY);
    }

    /**
     * Answers a lookup from the cache of the request as {@link RequestCacheAccess#memoized(Object, Supplier)} does, in
     * a new {@link ModificationProxy} of the type. The cache keeps the object without its proxy.
     */
    private static <T> @Nullable T memoizedProxy(@Nullable Object key, Class<T> type, Supplier<@Nullable T> lookup) {
        Supplier<@Nullable T> unwrappedLookup = () -> ModificationProxy.unwrap(lookup.get());
        T found = memoized(key, unwrappedLookup);
        return proxied(found, type);
    }

    /** A new proxy of the type over the object, null for null. */
    private static <T> @Nullable T proxied(@Nullable T object, Class<T> type) {
        if (object == null) {
            return null;
        }
        return ModificationProxy.create(object, type);
    }

    @Override
    public LoggingInfo getLogging() {
        return memoizedProxy(LOGGING_KEY, LoggingInfo.class, super::getLogging);
    }

    @Override
    public void setLogging(LoggingInfo logging) {
        super.setLogging(logging);
        evict(LOGGING_KEY);
    }

    @Override
    public void save(LoggingInfo logging) {
        super.save(logging);
        evict(LOGGING_KEY);
    }

    @Override
    public SettingsInfo getSettings(WorkspaceInfo workspace) {
        return memoizedProxy(settingsKey(workspace), SettingsInfo.class, () -> super.getSettings(workspace));
    }

    @Override
    public void add(SettingsInfo settings) {
        LookupKey key = settingsKey(settings.getWorkspace());
        super.add(settings);
        evict(key);
    }

    /**
     * Evicts the settings of the workspace before and after the save, as the save may set another workspace. The
     * settings are a {@link ModificationProxy}: the wrapped object holds the workspace from before the save, until the
     * facade below commits the proxy.
     */
    @Override
    public void save(SettingsInfo settings) {
        LookupKey before = settingsKey(ModificationProxy.unwrap(settings).getWorkspace());
        LookupKey after = settingsKey(settings.getWorkspace());
        super.save(settings);
        evict(before);
        evict(after);
    }

    @Override
    public void remove(SettingsInfo settings) {
        LookupKey key = settingsKey(settings.getWorkspace());
        super.remove(settings);
        evict(key);
    }

    /** The key of the settings of a workspace, or null without workspace id: the lookup then runs uncached. */
    private static @Nullable LookupKey settingsKey(@Nullable WorkspaceInfo workspace) {
        if (workspace == null || workspace.getId() == null) {
            return null;
        }
        return new LookupKey(SETTINGS_PREFIX + workspace.getId());
    }

    @Override
    public <T extends ServiceInfo> T getService(Class<T> clazz) {
        return memoizedProxy(serviceKey(null, clazz), clazz, () -> super.getService(clazz));
    }

    @Override
    public <T extends ServiceInfo> T getService(WorkspaceInfo workspace, Class<T> clazz) {
        return memoizedProxy(serviceKey(workspace, clazz), clazz, () -> super.getService(workspace, clazz));
    }

    /**
     * Caches the service under its id, whatever the class asked, and answers a lookup by another class with null, as
     * the facade below does. Only the miss of a lookup by {@code ServiceInfo} is cached: a lookup by another class
     * finding nothing does not tell whether a service has the id.
     */
    @Override
    public <T extends ServiceInfo> T getService(String id, Class<T> clazz) {
        RequestCache cache = liveCache();
        if (cache == null || id == null) {
            return super.getService(id, clazz);
        }
        IdKey key = new IdKey(ClassMappings.SERVICE, id);
        Optional<Object> cached = cache.found(key);
        if (cached.isPresent()) {
            T service = instanceOf(clazz, cached.get());
            return proxied(service, clazz);
        }
        T answer = super.getService(id, clazz);
        T found = ModificationProxy.unwrap(answer);
        if (found != null || ServiceInfo.class.equals(clazz)) {
            cache.put(key, found);
        }
        return proxied(found, clazz);
    }

    /** The cached object if it is an instance of the class, null for another class and for a miss. */
    private static <T> @Nullable T instanceOf(Class<T> clazz, Object cached) {
        if (clazz.isInstance(cached)) {
            return clazz.cast(cached);
        }
        return null;
    }

    @Override
    public <T extends ServiceInfo> T getServiceByName(String name, Class<T> clazz) {
        LookupKey key = serviceByNameKey(null, name, clazz);
        return memoizedProxy(key, clazz, () -> super.getServiceByName(name, clazz));
    }

    @Override
    public <T extends ServiceInfo> T getServiceByName(String name, WorkspaceInfo workspace, Class<T> clazz) {
        LookupKey key = serviceByNameKey(workspace, name, clazz);
        return memoizedProxy(key, clazz, () -> super.getServiceByName(name, workspace, clazz));
    }

    @Override
    public void add(ServiceInfo service) {
        super.add(service);
        evictKeysOf(service);
    }

    @Override
    public void save(ServiceInfo service) {
        super.save(service);
        evictKeysOf(service);
    }

    @Override
    public void remove(ServiceInfo service) {
        super.remove(service);
        evictKeysOf(service);
    }

    /**
     * Evicts the id key of a service, and the keys of the lookups of services by class and by name. A service answers
     * the lookups by several classes, such as {@code WMSInfo} and {@code ServiceInfo}, and the writes of services are
     * rare: evicting the lookups of the other services costs less than listing those classes.
     */
    private static void evictKeysOf(ServiceInfo service) {
        RequestCache cache = liveCache();
        if (cache != null) {
            cache.evict(new IdKey(ClassMappings.SERVICE, service.getId()));
            cache.evictIf(RequestCachingGeoServerFacade::isServiceLookup);
        }
    }

    private static boolean isServiceLookup(Object key) {
        if (key instanceof LookupKey lookup) {
            String discriminator = lookup.discriminator();
            return discriminator.startsWith(SERVICE_PREFIX) || discriminator.startsWith(SERVICE_NAME_PREFIX);
        }
        return false;
    }

    /** The key of a lookup of a service by class, in a workspace or among the global services. */
    private static @Nullable LookupKey serviceKey(@Nullable WorkspaceInfo workspace, @Nullable Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        return new LookupKey(SERVICE_PREFIX + scopeOf(workspace) + ":" + clazz.getName());
    }

    /** The key of a lookup of a service by name and class, in a workspace or among the global services. */
    private static @Nullable LookupKey serviceByNameKey(
            @Nullable WorkspaceInfo workspace, @Nullable String name, @Nullable Class<?> clazz) {
        if (name == null || clazz == null) {
            return null;
        }
        return new LookupKey(SERVICE_NAME_PREFIX + scopeOf(workspace) + ":" + name + ":" + clazz.getName());
    }

    /** The id of the workspace of a lookup of services, or {@code global} for the global services. */
    private static String scopeOf(@Nullable WorkspaceInfo workspace) {
        if (workspace == null) {
            return "global";
        }
        return workspace.getId();
    }
}
