/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.caching;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.plugin.forwarding.ForwardingGeoServerFacade;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

/** */
@CacheConfig(cacheNames = {"config"})
public class CachingGeoServerFacade extends ForwardingGeoServerFacade {

    public CachingGeoServerFacade(GeoServerFacade facade) {
        super(facade);
    }

    @Cacheable(key = "'global_geoserver'")
    public @Override GeoServerInfo getGlobal() {
        return super.getGlobal();
    }

    @CachePut(key = "'global_geoserver'")
    public @Override void setGlobal(GeoServerInfo global) {
        super.setGlobal(global);
    }

    @CacheEvict(key = "'global_geoserver'")
    public @Override void save(GeoServerInfo geoServer) {
        super.save(geoServer);
    }

    @Cacheable(key = "settings.#p0.id")
    public @Override SettingsInfo getSettings(WorkspaceInfo workspace) {
        return super.getSettings(workspace);
    }

    @CachePut(key = "settings.#p0.workspace.id")
    public @Override void add(SettingsInfo settings) {
        super.add(settings);
    }

    @CacheEvict(key = "settings.#p0.workspace.id")
    public @Override void save(SettingsInfo settings) {
        super.save(settings);
    }

    @CacheEvict(key = "settings.#p0.workspace.id")
    public @Override void remove(SettingsInfo settings) {
        super.remove(settings);
    }

    @Cacheable(key = "'global_logging'")
    public @Override LoggingInfo getLogging() {
        return super.getLogging();
    }

    @CachePut(key = "'global_logging'")
    public @Override void setLogging(LoggingInfo logging) {
        super.setLogging(logging);
    }

    @CacheEvict(key = "'global_logging'")
    public @Override void save(LoggingInfo logging) {
        super.save(logging);
    }

    @Cacheable(key = "service.#p0.id")
    public @Override void add(ServiceInfo service) {
        super.add(service);
    }

    @CacheEvict(key = "service.#p0.id")
    public @Override void remove(ServiceInfo service) {
        super.remove(service);
    }

    @CacheEvict(key = "service.#p0.id")
    public @Override void save(ServiceInfo service) {
        super.save(service);
    }

    //    public @Override <T extends ServiceInfo> T getService(Class<T> clazz) {
    //        return super.getService(clazz);
    //    }
    //
    //    public @Override <T extends ServiceInfo> T getService(WorkspaceInfo workspace, Class<T>
    // clazz) {
    //        return super.getService(workspace, clazz);
    //    }
    //
    //    public @Override <T extends ServiceInfo> T getService(String id, Class<T> clazz) {
    //        return super.getService(id, clazz);
    //    }
    //
    //    public @Override <T extends ServiceInfo> T getServiceByName(String name, Class<T> clazz) {
    //        return super.getServiceByName(name, clazz);
    //    }
    //
    //    public @Override <T extends ServiceInfo> T getServiceByName(
    //            String name, WorkspaceInfo workspace, Class<T> clazz) {
    //        return super.getServiceByName(name, workspace, clazz);
    //    }
}
