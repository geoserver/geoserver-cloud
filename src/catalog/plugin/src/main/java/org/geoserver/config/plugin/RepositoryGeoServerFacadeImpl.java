/*
 * (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.config.plugin;

import static java.util.Objects.requireNonNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.PropertyDiff;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.CoverageAccessInfoImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.util.logging.Logging;

import java.lang.reflect.Proxy;
import java.rmi.server.UID;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/**
 * Default implementation of {@link GeoServerFacade} backed by a pluggable {@link ConfigRepository}
 *
 * <p>
 *
 * @see MemoryConfigRepository
 */
public class RepositoryGeoServerFacadeImpl implements RepositoryGeoServerFacade {

    static final Logger LOGGER = Logging.getLogger(RepositoryGeoServerFacadeImpl.class);

    protected ConfigRepository repository;

    protected GeoServer geoServer;

    public RepositoryGeoServerFacadeImpl() {
        this(new MemoryConfigRepository());
    }

    public RepositoryGeoServerFacadeImpl(ConfigRepository repository) {
        Objects.requireNonNull(repository);
        this.repository = repository;
    }

    public @Override void setRepository(ConfigRepository repository) {
        requireNonNull(repository);
        this.repository = repository;
    }

    public @Override void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    public @Override GeoServer getGeoServer() {
        return geoServer;
    }

    public @Override GeoServerInfo getGlobal() {
        return wrap(resolve(repository.getGlobal().orElse(null)), GeoServerInfo.class);
    }

    public @Override void setGlobal(GeoServerInfo global) {
        resolve(global);
        setId(global.getSettings());
        repository.setGlobal(global);
    }

    public @Override void save(GeoServerInfo global) {
        ModificationProxy proxy = (ModificationProxy) Proxy.getInvocationHandler(global);

        List<String> propertyNames = proxy.getPropertyNames();
        List<Object> oldValues = proxy.getOldValues();
        List<Object> newValues = proxy.getNewValues();

        geoServer.fireGlobalModified(global, propertyNames, oldValues, newValues);

        proxy.commit();
        repository.setGlobal(unwrap(global));
    }

    public @Override SettingsInfo getSettings(WorkspaceInfo workspace) {
        requireNonNull(workspace);
        return wrap(
                resolve(repository.getSettingsByWorkspace(workspace).orElse(null)),
                SettingsInfo.class);
    }

    public @Override void add(SettingsInfo s) {
        s = unwrap(s);
        setId(s);
        repository.add(s);
    }

    public @Override void save(SettingsInfo settings) {
        ModificationProxy proxy = (ModificationProxy) Proxy.getInvocationHandler(settings);

        List<String> propertyNames = proxy.getPropertyNames();
        List<Object> oldValues = proxy.getOldValues();
        List<Object> newValues = proxy.getNewValues();

        settings = (SettingsInfo) proxy.getProxyObject();
        geoServer.fireSettingsModified(settings, propertyNames, oldValues, newValues);

        Patch patch = PropertyDiff.valueOf(proxy).clean().toPatch();
        repository.update(ModificationProxy.unwrap(settings), patch);

        proxy.commit();
    }

    public @Override void remove(SettingsInfo s) {
        s = unwrap(s);
        repository.remove(s);
    }

    public @Override LoggingInfo getLogging() {
        return wrap(repository.getLogging().orElse(null), LoggingInfo.class);
    }

    public @Override void setLogging(LoggingInfo logging) {
        requireNonNull(logging);
        repository.setLogging(logging);
    }

    public @Override void save(LoggingInfo logging) {
        ModificationProxy proxy = (ModificationProxy) Proxy.getInvocationHandler(logging);

        List<String> propertyNames = proxy.getPropertyNames();
        List<Object> oldValues = proxy.getOldValues();
        List<Object> newValues = proxy.getNewValues();

        geoServer.fireLoggingModified(logging, propertyNames, oldValues, newValues);

        proxy.commit();
        repository.setLogging(unwrap(logging));
    }

    public @Override void add(ServiceInfo service) {
        // may be adding a proxy, need to unwrap
        service = unwrap(service);
        setId(service);
        service.setGeoServer(geoServer);

        repository.add(service);
    }

    public @Override void save(ServiceInfo service) {
        ModificationProxy proxy = ModificationProxy.handler(service);

        List<String> propertyNames = proxy.getPropertyNames();
        List<Object> oldValues = proxy.getOldValues();
        List<Object> newValues = proxy.getNewValues();

        geoServer.fireServiceModified(service, propertyNames, oldValues, newValues);
        Patch patch = PropertyDiff.valueOf(proxy).clean().toPatch();
        repository.update(ModificationProxy.unwrap(service), patch);

        proxy.commit();
    }

    public @Override void remove(ServiceInfo service) {
        repository.remove(service);
    }

    public @Override <T extends ServiceInfo> T getService(Class<T> clazz) {
        return find(clazz, null);
    }

    public @Override <T extends ServiceInfo> T getService(WorkspaceInfo workspace, Class<T> clazz) {
        return find(clazz, workspace);
    }

    public @Override <T extends ServiceInfo> T getService(String id, Class<T> type) {
        requireNonNull(id);
        requireNonNull(type);
        Optional<T> service = repository.getServiceById(id, type);
        if (service.isEmpty() && LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Could not locate service of type " + type + " and id '" + id);
        }
        return wrap(resolve(service.orElse(null)), type);
    }

    public @Override <T extends ServiceInfo> T getServiceByName(String name, Class<T> clazz) {
        return findByName(name, null, clazz);
    }

    public @Override <T extends ServiceInfo> T getServiceByName(
            String name, WorkspaceInfo workspace, Class<T> clazz) {
        return findByName(name, workspace, clazz);
    }

    public @Override Collection<? extends ServiceInfo> getServices() {
        List<ServiceInfo> all =
                repository.getGlobalServices().map(this::resolve).collect(Collectors.toList());
        return ModificationProxy.createList(all, ServiceInfo.class);
    }

    public @Override Collection<? extends ServiceInfo> getServices(WorkspaceInfo workspace) {
        List<ServiceInfo> services =
                repository
                        .getServicesByWorkspace(workspace)
                        .map(this::resolve)
                        .collect(Collectors.toList());
        return ModificationProxy.createList(services, ServiceInfo.class);
    }

    public @Override void dispose() {
        repository.dispose();
    }

    public static <T> T wrap(T info, Class<T> clazz) {
        return info == null ? null : ModificationProxy.create(info, clazz);
    }

    public static <T> T unwrap(T obj) {
        return ModificationProxy.unwrap(obj);
    }

    protected GeoServerInfo resolve(GeoServerInfo info) {
        if (info == null) return null;
        GeoServerInfoImpl global = (GeoServerInfoImpl) info;
        if (global.getMetadata() == null) {
            global.setMetadata(new MetadataMap());
        }
        if (global.getClientProperties() == null) {
            global.setClientProperties(new HashMap<Object, Object>());
        }
        if (global.getCoverageAccess() == null) {
            global.setCoverageAccess(new CoverageAccessInfoImpl());
        }
        OwsUtils.resolveCollections(global);
        return info;
    }

    protected <T extends ServiceInfo> T find(Class<T> type, @Nullable WorkspaceInfo workspace) {
        Optional<T> service;
        if (workspace == null) {
            service = repository.getGlobalService(type);
        } else {
            requireNonNull(workspace.getId());
            service = repository.getServiceByWorkspace(workspace, type);
        }
        if (service.isEmpty() && LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Could not locate service of type " + type + " in workspace " + workspace);
        }

        return wrap(resolve(service.orElse(null)), type);
    }

    protected <T extends ServiceInfo> T findByName(
            String name, WorkspaceInfo workspace, Class<T> type) {
        Optional<T> service;
        if (workspace == null) {
            service = repository.getServiceByName(name, type);
        } else {
            requireNonNull(workspace.getId());
            service = repository.getServiceByNameAndWorkspace(name, workspace, type);
        }
        if (service.isEmpty() && LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "Could not locate service of type "
                            + type
                            + " in workspace "
                            + workspace
                            + " and name '"
                            + name
                            + "'");
        }
        return wrap(resolve(service.orElse(null)), type);
    }

    protected void setId(Object o) {
        if (OwsUtils.get(o, "id") == null) {
            String uid = new UID().toString();
            OwsUtils.set(o, "id", o.getClass().getSimpleName() + "-" + uid);
        }
    }

    protected <S extends ServiceInfo> S resolve(S service) {
        if (service == null) return null;
        WorkspaceInfo workspace = service.getWorkspace();
        GeoServer gs = getGeoServer();
        if (workspace instanceof Proxy) {
            Catalog catalog = gs.getCatalog();
            WorkspaceInfo resolved = ResolvingProxy.resolve(catalog, workspace);
            service.setWorkspace(resolved);
        }
        service.setGeoServer(gs);
        OwsUtils.resolveCollections(service);
        return service;
    }

    protected SettingsInfo resolve(SettingsInfo settings) {
        if (settings == null) return null;
        WorkspaceInfo workspace = settings.getWorkspace();
        if (workspace instanceof Proxy) {
            GeoServer gs = getGeoServer();
            Catalog catalog = gs.getCatalog();
            WorkspaceInfo resolved = ResolvingProxy.resolve(catalog, workspace);
            settings.setWorkspace(resolved);
        }
        return settings;
    }

    @Override
    public SettingsInfo getSettings(String id) {
        return repository.getSettingsById(id).orElse(null);
    }
}
