/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.catalog.cache;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.cloud.event.UpdateSequenceEvent;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.plugin.forwarding.ForwardingGeoServerFacade;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;

/** */
@CacheConfig(cacheNames = CachingGeoServerFacade.CACHE_NAME)
@Slf4j(topic = "org.geoserver.cloud.catalog.caching")
public class CachingGeoServerFacade extends ForwardingGeoServerFacade {

    /**
     * Name of the cache used by {@link CachingGeoServerFacade}, used as key to acquire it through
     * {@link CacheManager#getCache(String)}
     */
    static final String CACHE_NAME = "gs-config";

    /** Key used to cache and evict the {@link GeoServerInfo global configuration} object */
    static final String GEOSERVERINFO_KEY = "global_GeoServer";

    /** Key used to cache and evict the {@link LoggingInfo global logging} settings object */
    static final String LOGGINGINFO_KEY = "global_Logging";

    /** Key used to cache and evict the {@link #getServices() global services} list */
    static final String GLOBAL_SERVICES_KEY = "global_services";

    private final @NonNull Cache cache;

    public CachingGeoServerFacade(@NonNull GeoServerFacade facade, @NonNull Cache cache) {
        super(facade);
        this.cache = cache;
    }

    ////// Event handling ///////

    /**
     * Clears the whole config cache upon any {@link UpdateSequenceEvent}.
     *
     * <p>{@link UpdateSequenceEvent} is the root event for the ones that change something in the
     * catalog or the configuration.
     */
    @EventListener(classes = UpdateSequenceEvent.class)
    void onUpdateSequenceEvent(UpdateSequenceEvent event) {
        if (event.isRemote()) {
            evictAll();
        }
    }

    ////// Cache manipulation functions ///////
    <T extends ServiceInfo> T cachePutIncludeNull(@NonNull Object key, @NonNull Cache cache, T service) {

        if (service == null) {
            cache.put(key, null);
            return null;
        }
        return cachePut(cache, service);
    }

    <T extends ServiceInfo> T cachePut(@NonNull Cache cache, @NonNull T service) {
        WorkspaceInfo ws = service.getWorkspace();

        Object byId = CachingGeoServerFacade.serviceByIdKey(service.getId());
        Object byName = CachingGeoServerFacade.serviceByNameKey(ws, service.getName());
        Object byType = CachingGeoServerFacade.serviceByTypeKey(ws, service.getClass());

        cache.put(byId, service);
        cache.put(byName, service);
        cache.put(byType, service);
        log.debug("Cached entry for service {}", service.getId());
        log.trace("cached keys = id: {}, name: {}, type: {}", byId, byName, byType);
        return service;
    }

    ////// GeoServerFacade functions ///////

    @Override
    @Cacheable(key = "'" + GEOSERVERINFO_KEY + "'", unless = "#result == null")
    public GeoServerInfo getGlobal() {
        return super.getGlobal();
    }

    @Override
    @CacheEvict(allEntries = true)
    public void setGlobal(GeoServerInfo global) {
        super.setGlobal(global);
    }

    @Override
    @CacheEvict(allEntries = true)
    public void save(GeoServerInfo geoServer) {
        super.save(geoServer);
    }

    @Override
    public SettingsInfo getSettings(WorkspaceInfo workspace) {
        SettingsInfo settings;
        Object key = CachingGeoServerFacade.settingsKey(workspace);
        ValueWrapper cached = cache.get(key);
        if (cached == null) {
            settings = super.getSettings(workspace);
            cache.put(key, settings); // cache even if null value
            if (settings != null) {
                cache.put(settings.getId(), settings);
            }
        } else {
            settings = (SettingsInfo) cached.get();
        }
        return settings;
    }

    @Override
    @CacheEvict(allEntries = true)
    public void add(SettingsInfo settings) {
        super.add(settings);
    }

    @Override
    @CacheEvict(allEntries = true)
    public void save(SettingsInfo settings) {
        super.save(settings);
    }

    @Override
    @CacheEvict(allEntries = true)
    public void remove(SettingsInfo settings) {
        super.remove(settings);
    }

    @Override
    @Cacheable(key = "'" + LOGGINGINFO_KEY + "'", unless = "#result == null")
    public LoggingInfo getLogging() {
        return super.getLogging();
    }

    @Override
    @CacheEvict(allEntries = true)
    public void setLogging(LoggingInfo logging) {
        super.setLogging(logging);
    }

    @Override
    @CacheEvict(allEntries = true)
    public void save(LoggingInfo logging) {
        super.save(logging);
    }

    @Override
    @CacheEvict(allEntries = true)
    public void add(ServiceInfo service) {
        super.add(service);
    }

    @Override
    @CacheEvict(allEntries = true)
    public void remove(ServiceInfo service) {
        super.remove(service);
    }

    @Override
    @CacheEvict(allEntries = true)
    public void save(ServiceInfo service) {
        super.save(service);
    }

    @Override
    public <T extends ServiceInfo> T getService(Class<T> clazz) {
        Object key = CachingGeoServerFacade.serviceByTypeKey(null, clazz);
        ValueWrapper value = cache.get(key);
        ServiceInfo service;
        if (value == null) {
            service = cachePutIncludeNull(key, cache, super.getService(clazz));
        } else {
            service = (ServiceInfo) value.get();
        }
        return clazz.isInstance(service) ? clazz.cast(service) : null;
    }

    @Override
    public <T extends ServiceInfo> T getService(WorkspaceInfo workspace, Class<T> clazz) {
        Object byTypeKey = CachingGeoServerFacade.serviceByTypeKey(workspace, clazz);
        ValueWrapper value = cache.get(byTypeKey);
        ServiceInfo service;
        if (value == null) {
            service = cachePutIncludeNull(byTypeKey, cache, super.getService(workspace, clazz));
        } else {
            service = (ServiceInfo) value.get();
        }
        return clazz.isInstance(service) ? clazz.cast(service) : null;
    }

    @Override
    public <T extends ServiceInfo> T getService(String id, Class<T> clazz) {
        Object key = CachingGeoServerFacade.serviceByIdKey(id);
        ValueWrapper value = cache.get(key);
        ServiceInfo service;
        if (value == null) {
            service = cachePutIncludeNull(key, cache, super.getService(id, clazz));
        } else {
            service = (ServiceInfo) value.get();
        }
        return clazz.isInstance(service) ? clazz.cast(service) : null;
    }

    @Override
    public <T extends ServiceInfo> T getServiceByName(String name, Class<T> clazz) {
        Object key = CachingGeoServerFacade.serviceByNameKey((WorkspaceInfo) null, name);
        ValueWrapper value = cache.get(key);
        ServiceInfo service;
        if (value == null) {
            service = cachePutIncludeNull(key, cache, super.getServiceByName(name, clazz));
        } else {
            service = (ServiceInfo) value.get();
        }
        return clazz.isInstance(service) ? clazz.cast(service) : null;
    }

    @Override
    public <T extends ServiceInfo> T getServiceByName(String name, WorkspaceInfo workspace, Class<T> clazz) {

        Object key = CachingGeoServerFacade.serviceByNameKey(workspace, name);
        ValueWrapper value = cache.get(key);
        ServiceInfo service;
        if (value == null) {
            service = cachePutIncludeNull(key, cache, super.getServiceByName(name, workspace, clazz));
        } else {
            service = (ServiceInfo) value.get();
        }
        return clazz.isInstance(service) ? clazz.cast(service) : null;
    }

    @Override
    @Cacheable(key = "'" + GLOBAL_SERVICES_KEY + "'", unless = "#result.isEmpty()")
    public Collection<? extends ServiceInfo> getServices() {
        return super.getServices();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<? extends ServiceInfo> getServices(WorkspaceInfo workspace) {
        Object key = CachingGeoServerFacade.servicesByWorkspaceKey(workspace);
        Collection<? extends ServiceInfo> services;
        ValueWrapper cached = cache.get(key);
        if (cached == null) {
            services = super.getServices(workspace);
            services = services == null ? List.of() : List.copyOf(services);
            cache.put(key, services);
        } else {
            services = (Collection<? extends ServiceInfo>) cached.get();
        }
        return services;
    }

    /**
     * Method used to build a cache key for the {@link SettingsInfo settings} of a given workspace
     */
    public static Object settingsKey(WorkspaceInfo ws) {
        return "settings@" + ws.getId();
    }

    public static Object servicesByWorkspaceKey(@NonNull WorkspaceInfo ws) {
        return "services@" + ws.getId();
    }

    public static Object serviceByIdKey(String id) {
        return ServiceInfoKey.byId(id);
    }

    public static Object serviceByNameKey(@Nullable WorkspaceInfo ws, @NonNull String name) {
        return ServiceInfoKey.byName(ws, name);
    }

    public static Object serviceByTypeKey(@Nullable WorkspaceInfo ws, @NonNull Class<? extends ServiceInfo> type) {
        return ServiceInfoKey.byType(ws, type);
    }

    public void evictAll() {
        cache.clear();
    }
}
