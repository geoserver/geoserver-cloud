/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.caching;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.plugin.forwarding.ForwardingGeoServerFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

/** */
@CacheConfig(cacheNames = CachingGeoServerFacade.CACHE_NAME)
@Slf4j(topic = "org.geoserver.cloud.catalog.caching")
public class CachingGeoServerFacadeImpl extends ForwardingGeoServerFacade
        implements CachingGeoServerFacade {

    private Cache cache;

    @Override
    public boolean evict(Info info) {
        log.debug("Evict cache entry for {}", info.getId());
        if (info instanceof GeoServerInfo) {
            return cache.evictIfPresent(GEOSERVERINFO_KEY);
        }
        if (info instanceof LoggingInfo) {
            return cache.evictIfPresent(LOGGINGINFO_KEY);
        }
        if (info instanceof SettingsInfo) {
            String id = info.getId();
            ValueWrapper cachedValue = cache.get(id);
            cache.evict(id);
            if (cachedValue != null) {
                SettingsInfo cached = (SettingsInfo) cachedValue.get();
                if (cached != null && cached.getWorkspace() != null) {
                    Object wsKey = CachingGeoServerFacade.settingsKey(cached.getWorkspace());
                    cache.evict(wsKey);
                }
                return true;
            }
        }
        if (info instanceof ServiceInfo) {
            ServiceInfo service = (ServiceInfo) info;
            Object idKey = CachingGeoServerFacade.serviceByIdKey(service.getId());
            ValueWrapper cachedValue = cache.get(idKey);
            if (cachedValue != null) {
                ServiceInfo cached = (ServiceInfo) cachedValue.get();
                cache.evict(idKey);
                if (cached != null) {
                    WorkspaceInfo ws = cached.getWorkspace();
                    Object nameKey = CachingGeoServerFacade.serviceByNameKey(ws, cached.getName());
                    Object typeKey = CachingGeoServerFacade.serviceByTypeKey(ws, cached.getClass());
                    cache.evict(nameKey);
                    cache.evict(typeKey);
                }
                return true;
            }
        }
        return false;
    }

    static <T extends ServiceInfo> T cachePutIncludeNull(
            @NonNull Object key, @NonNull Cache cache, T service) {

        if (service == null) {
            cache.put(key, null);
            return null;
        }
        return cachePut(cache, service);
    }

    static <T extends ServiceInfo> T cachePut(@NonNull Cache cache, @NonNull T service) {
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

    public CachingGeoServerFacadeImpl(GeoServerFacade facade) {
        super(facade);
    }

    public @Autowired void setCacheManager(CacheManager cacheManager) {
        cache = cacheManager.getCache(CACHE_NAME);
    }

    @Cacheable(key = "'" + GEOSERVERINFO_KEY + "'", unless = "#result == null")
    public @Override GeoServerInfo getGlobal() {
        return super.getGlobal();
    }

    @CacheEvict(key = "'" + GEOSERVERINFO_KEY + "'")
    public @Override void setGlobal(GeoServerInfo global) {
        super.setGlobal(global);
    }

    @CacheEvict(key = "'" + GEOSERVERINFO_KEY + "'")
    public @Override void save(GeoServerInfo geoServer) {
        super.save(geoServer);
    }

    public @Override SettingsInfo getSettings(WorkspaceInfo workspace) {
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

    @Caching(
            evict = {
                @CacheEvict(key = "#settings.id"),
                @CacheEvict(key = "'settings@' + #settings.workspace.id")
            })
    public @Override void save(SettingsInfo settings) {
        super.save(settings);
    }

    @Caching(
            evict = {
                @CacheEvict(key = "#settings.id"),
                @CacheEvict(key = "'settings@' + #settings.workspace.id")
            })
    public @Override void remove(SettingsInfo settings) {
        super.remove(settings);
    }

    @Cacheable(key = "'" + LOGGINGINFO_KEY + "'", unless = "#result == null")
    public @Override LoggingInfo getLogging() {
        return super.getLogging();
    }

    @CacheEvict(key = "'" + LOGGINGINFO_KEY + "'")
    public @Override void setLogging(LoggingInfo logging) {
        super.setLogging(logging);
    }

    @CacheEvict(key = "'" + LOGGINGINFO_KEY + "'")
    public @Override void save(LoggingInfo logging) {
        super.save(logging);
    }

    public @Override void remove(ServiceInfo service) {
        evict(service);
        super.remove(service);
    }

    public @Override void save(ServiceInfo service) {
        evict(service);
        super.save(service);
    }

    public @Override <T extends ServiceInfo> T getService(Class<T> clazz) {
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

    public @Override <T extends ServiceInfo> T getService(WorkspaceInfo workspace, Class<T> clazz) {
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

    public @Override <T extends ServiceInfo> T getService(String id, Class<T> clazz) {
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

    public @Override <T extends ServiceInfo> T getServiceByName(String name, Class<T> clazz) {
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

    public @Override <T extends ServiceInfo> T getServiceByName(
            String name, WorkspaceInfo workspace, Class<T> clazz) {

        Object key = CachingGeoServerFacade.serviceByNameKey(workspace, name);
        ValueWrapper value = cache.get(key);
        ServiceInfo service;
        if (value == null) {
            service =
                    cachePutIncludeNull(key, cache, super.getServiceByName(name, workspace, clazz));
        } else {
            service = (ServiceInfo) value.get();
        }
        return clazz.isInstance(service) ? clazz.cast(service) : null;
    }
}
