/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.cache;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.cloud.event.UpdateSequenceEvent;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.cloud.event.config.GeoServerInfoModified;
import org.geoserver.cloud.event.config.GeoServerInfoSet;
import org.geoserver.cloud.event.config.LoggingInfoModified;
import org.geoserver.cloud.event.config.LoggingInfoSet;
import org.geoserver.cloud.event.config.ServiceModified;
import org.geoserver.cloud.event.config.ServiceRemoved;
import org.geoserver.cloud.event.config.SettingsModified;
import org.geoserver.cloud.event.config.SettingsRemoved;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.config.plugin.forwarding.ForwardingGeoServerFacade;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.event.EventListener;

import java.util.Optional;

import javax.annotation.Nullable;

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

    private final @NonNull Cache cache;

    public CachingGeoServerFacade(@NonNull GeoServerFacade facade, @NonNull Cache cache) {
        super(facade);
        this.cache = cache;
    }

    ////// Event handling ///////

    /** Evicts the {@link GeoServerInfo global config} upon any update sequence modifying event. */
    @EventListener(classes = UpdateSequenceEvent.class)
    void onUpdateSequenceEvent(UpdateSequenceEvent event) {
        evictGlobal()
                .ifPresent(
                        evicted ->
                                log.debug(
                                        "evicted global config with updatesequence {} upon event carrying update sequence {}",
                                        evicted.getUpdateSequence(),
                                        event.getUpdateSequence()));
    }

    @EventListener(classes = GeoServerInfoModified.class)
    void onGeoServerInfoModifyEvent(GeoServerInfoModified event) {
        evictConfigEntry(event);
    }

    @EventListener(classes = LoggingInfoModified.class)
    void onLoggingInfoModifyEvent(LoggingInfoModified event) {
        evictConfigEntry(event);
    }

    @EventListener(classes = SettingsModified.class)
    void onSettingsInfoModifyEvent(SettingsModified event) {
        evictConfigEntry(event);
    }

    @EventListener(classes = ServiceModified.class)
    void onServiceInfoModifyEvent(ServiceModified event) {
        evictConfigEntry(event);
    }

    @EventListener(classes = SettingsRemoved.class)
    void onSettingsInfoRemoveEvent(SettingsRemoved event) {
        evictConfigEntry(event);
    }

    @EventListener(classes = ServiceRemoved.class)
    void onServiceInfoRemoveEvent(ServiceRemoved event) {
        evictConfigEntry(event);
    }

    @EventListener(classes = GeoServerInfoSet.class)
    void onGeoServerInfoSetEvent(GeoServerInfoSet event) {
        evictConfigEntry(event);
    }

    @EventListener(classes = LoggingInfoSet.class)
    void onLoggingInfoSetEvent(LoggingInfoSet event) {
        evictConfigEntry(event);
    }

    /**
     * Can't know which {@link ServiceInfo} are attached to the workspace when its removed, so it
     * invalidates all cache entries, possibly asynchronously, as per {@link Cache#clear()}
     *
     * <p>As opposed to the other event handler methods, this one evicts upon both {@link
     * InfoEvent#isRemote() remote} and {@link InfoEvent#isLocal() local} events.
     */
    @EventListener(classes = CatalogInfoRemoved.class)
    void onWorkspaceRemoved(CatalogInfoRemoved event) {
        if (event.getObjectType() == ConfigInfoType.WORKSPACE) {
            cache.clear();
        }
    }

    public void evictConfigEntry(InfoEvent event) {
        if (!event.isRemote()) return;

        String objectId = event.getObjectId();
        ConfigInfoType infoType = event.getObjectType();
        Info info;
        if (event instanceof SettingsModified se) {
            info = mockSettings(se.getObjectId(), se.getWorkspaceId());
        } else if (event instanceof SettingsRemoved se) {
            info = mockSettings(se.getObjectId(), se.getWorkspaceId());
        } else {
            info = ResolvingProxy.create(objectId, infoType.getType());
        }

        if (evict(info)) {
            log.debug("Evicted config cache entry due to {}", event);
        } else {
            log.trace("Remote event resulted in no cache eviction: {}", event);
        }
    }

    private SettingsInfo mockSettings(@NonNull String objectId, String workspaceId) {
        var s = new SettingsInfoImpl();
        s.setId(objectId);
        s.setWorkspace(ResolvingProxy.create(workspaceId, WorkspaceInfo.class));
        return s;
    }

    ////// Cache manipulation functions ///////

    Optional<GeoServerInfo> evictGlobal() {
        Optional<GeoServerInfo> ret =
                Optional.ofNullable(cache.get(GEOSERVERINFO_KEY))
                        .map(ValueWrapper::get)
                        .map(GeoServerInfo.class::cast);
        cache.evict(GEOSERVERINFO_KEY);
        return ret;
    }

    boolean evict(Info info) {
        log.debug("Evict cache entry for {}", info.getId());
        if (info instanceof GeoServerInfo) {
            return evictGlobal().isPresent();
        }
        if (info instanceof LoggingInfo) {
            return cache.evictIfPresent(LOGGINGINFO_KEY);
        }
        if (info instanceof SettingsInfo settings) {
            return evict(settings);
        }
        if (info instanceof ServiceInfo service) {
            return evict(service);
        }
        return false;
    }

    private boolean evict(SettingsInfo settings) {
        String id = settings.getId();
        boolean evicted = cache.evictIfPresent(id);
        WorkspaceInfo workspace = settings.getWorkspace();
        if (workspace != null) {
            Object wsKey = CachingGeoServerFacade.settingsKey(workspace);
            evicted |= cache.evictIfPresent(wsKey);
        }
        return evicted;
    }

    private boolean evict(ServiceInfo service) {
        Object idKey = CachingGeoServerFacade.serviceByIdKey(service.getId());
        ValueWrapper cachedValue = cache.get(idKey);
        if (cachedValue == null) return false;
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

    <T extends ServiceInfo> T cachePutIncludeNull(
            @NonNull Object key, @NonNull Cache cache, T service) {

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
    @CacheEvict(key = "'" + GEOSERVERINFO_KEY + "'", beforeInvocation = true)
    public void setGlobal(GeoServerInfo global) {
        super.setGlobal(global);
    }

    @Override
    @CacheEvict(key = "'" + GEOSERVERINFO_KEY + "'", beforeInvocation = true)
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
    @Caching(
            evict = {
                @CacheEvict(key = "#settings.id", beforeInvocation = true),
                @CacheEvict(key = "'settings@' + #settings.workspace.id", beforeInvocation = true)
            })
    public void save(SettingsInfo settings) {
        super.save(settings);
    }

    @Override
    @Caching(
            evict = {
                @CacheEvict(key = "#settings.id", beforeInvocation = true),
                @CacheEvict(key = "'settings@' + #settings.workspace.id", beforeInvocation = true)
            })
    public void remove(SettingsInfo settings) {
        super.remove(settings);
    }

    @Override
    @Cacheable(key = "'" + LOGGINGINFO_KEY + "'", unless = "#result == null")
    public LoggingInfo getLogging() {
        return super.getLogging();
    }

    @Override
    @CacheEvict(key = "'" + LOGGINGINFO_KEY + "'", beforeInvocation = true)
    public void setLogging(LoggingInfo logging) {
        super.setLogging(logging);
    }

    @Override
    @CacheEvict(key = "'" + LOGGINGINFO_KEY + "'", beforeInvocation = true)
    public void save(LoggingInfo logging) {
        super.save(logging);
    }

    @Override
    public void remove(ServiceInfo service) {
        evict(service);
        super.remove(service);
    }

    @Override
    public void save(ServiceInfo service) {
        evict(service);
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
    public <T extends ServiceInfo> T getServiceByName(
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

    /**
     * Method used to build a cache key for the {@link SettingsInfo settings} of a given workspace
     */
    public static Object settingsKey(WorkspaceInfo ws) {
        return "settings@" + ws.getId();
    }

    public static Object serviceByIdKey(String id) {
        return ServiceInfoKey.byId(id);
    }

    public static Object serviceByNameKey(@Nullable WorkspaceInfo ws, @NonNull String name) {
        return ServiceInfoKey.byName(ws, name);
    }

    public static Object serviceByTypeKey(
            @Nullable WorkspaceInfo ws, @NonNull Class<? extends ServiceInfo> type) {
        return ServiceInfoKey.byType(ws, type);
    }
}
