/*
 * (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.config.plugin;

import static org.geoserver.ows.util.OwsUtils.resolveCollections;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.LocalWorkspaceCatalog;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.config.*;
import org.geoserver.config.impl.GeoServerFactoryImpl;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.referencing.CRS;
import org.geotools.util.decorate.Wrapper;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of GeoServer global and service configuration manager.
 *
 * <p>Implementation details:
 *
 * <ul>
 *   <li>Upon config changes, handles all event publishing to its {@link #getListeners() listeners}
 *   <li>Ensures no config {@link Info} object leaves a query method without being wrapped in a
 *       {@link ModificationProxy}
 *   <li>Relies on a provided {@link #setFacade GeoServerFacade} to access raw configuration
 *       objects, makes no assumptions over their origin and persistence mechanism (or lack of). All
 *       config objects obtained from the {@link GeoServerFacade} are treated as transient; {@link
 *       ModificationProxy} is used to resolve changes and ask the {@link GeoServerFacade} to apply
 *       a {@link Patch} to the live value of that object
 *   <li>References to {@link Info} objects (catalog's {@link WorkspaceInfo} references, for
 *       instance) are resolved before returning from this class. {@link GeoServerFacade}
 *       implementation is free to return {@link ResolvingProxy} wrappers for those objects, in
 *       order to avoid holding onto stale instances or having to deal with resolving them and hence
 *       accessing the {@link Catalog} outside their level of abstraction.
 * </ul>
 */
public class GeoServerImpl implements GeoServer, ApplicationContextAware {

    private static final Logger LOGGER = Logging.getLogger(GeoServerImpl.class);

    /** factory for creating objects */
    private GeoServerFactory factory = new GeoServerFactoryImpl(this);

    /** the catalog */
    private Catalog catalog;

    /** data access object */
    private GeoServerFacade facade;

    /** listeners */
    private List<ConfigurationListener> listeners = new ArrayList<>();

    public GeoServerImpl() {
        this(new RepositoryGeoServerFacadeImpl());
    }

    public GeoServerImpl(GeoServerFacade facade) {
        setFacade(facade);
    }

    @Override
    public GeoServerFacade getFacade() {
        return facade;
    }

    public void setFacade(GeoServerFacade facade) {
        this.facade = facade;
        facade.setGeoServer(this);
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        if (factory instanceof ApplicationContextAware appcAware) {
            appcAware.setApplicationContext(context);
        }
    }

    @Override
    public GeoServerFactory getFactory() {
        return factory;
    }

    @Override
    public void setFactory(GeoServerFactory factory) {
        this.factory = factory;
    }

    @Override
    public Catalog getCatalog() {
        return catalog;
    }

    @Override
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;

        // This instance of check is has to be here because this Geoserver cannot be injected
        // into LocalWorkspaceCatalog because it causes a circular reference
        if (catalog instanceof LocalWorkspaceCatalog lwCatalog) {
            lwCatalog.setGeoServer(this);
        }
    }

    @Override
    public GeoServerInfo getGlobal() {
        return facade.getGlobal();
    }

    @Override
    public void setGlobal(GeoServerInfo global) {
        facade.setGlobal(global);

        // fire the modification event
        fireGlobalPostModified();
    }

    @Override
    public SettingsInfo getSettings() {
        SettingsInfo settings = null;
        if (LocalWorkspace.get() != null) {
            settings = getSettings(LocalWorkspace.get());
        }
        return settings != null ? settings : getGlobal().getSettings();
    }

    @Override
    public SettingsInfo getSettings(WorkspaceInfo workspace) {
        return facade.getSettings(workspace);
    }

    @Override
    public void add(SettingsInfo settings) {
        validate(settings);
        resolve(settings);

        WorkspaceInfo workspace = settings.getWorkspace();
        if (facade.getSettings(workspace) != null) {
            throw new IllegalArgumentException(
                    "Settings already exist for workspace '%s'".formatted(workspace.getName()));
        }

        facade.add(settings);
        fireSettingsAdded(settings);
    }

    @Override
    public void save(SettingsInfo settings) {
        validate(settings);

        facade.save(settings);
        fireSettingsPostModified(settings);
    }

    @Override
    public void remove(SettingsInfo settings) {
        facade.remove(settings);

        fireSettingsRemoved(settings);
    }

    void validate(SettingsInfo settings) {
        final WorkspaceInfo workspace = settings.getWorkspace();
        if (workspace == null) {
            throw new IllegalArgumentException("Settings must be part of a workspace");
        }
        // make sure the workspace exists and is not dettached from the catalog
        final WorkspaceInfo realws = getCatalog().getWorkspace(workspace.getId());
        Objects.requireNonNull(
                realws,
                () ->
                        "Workspace %s(%s) attached to SettingsInfo does not exist"
                                .formatted(workspace.getName(), workspace.getId()));
        settings.setWorkspace(realws);
    }

    void resolve(SettingsInfo settings) {
        resolveCollections(settings);
    }

    void fireSettingsAdded(SettingsInfo settings) {
        for (ConfigurationListener l : listeners) {
            try {
                l.handleSettingsAdded(settings);
            } catch (RuntimeException e) {
                logListenerError(e);
            }
        }
    }

    @Override
    public void fireSettingsModified(
            SettingsInfo settings,
            List<String> changed,
            List<Object> oldValues,
            List<Object> newValues) {
        for (ConfigurationListener l : listeners) {
            try {
                l.handleSettingsModified(settings, changed, oldValues, newValues);
            } catch (RuntimeException e) {
                logListenerError(e);
            }
        }
    }

    void fireSettingsPostModified(SettingsInfo settings) {
        for (ConfigurationListener l : listeners) {
            try {
                l.handleSettingsPostModified(settings);
            } catch (RuntimeException e) {
                logListenerError(e);
            }
        }
    }

    void fireSettingsRemoved(SettingsInfo settings) {
        for (ConfigurationListener l : listeners) {
            try {
                l.handleSettingsRemoved(settings);
            } catch (RuntimeException e) {
                logListenerError(e);
            }
        }
    }

    @Override
    public LoggingInfo getLogging() {
        return facade.getLogging();
    }

    @Override
    public void setLogging(LoggingInfo logging) {
        facade.setLogging(logging);
        fireLoggingPostModified();
    }

    @Override
    public void add(ServiceInfo service) {
        if (service.getId() != null
                && facade.getService(service.getId(), ServiceInfo.class) != null) {
            throw new IllegalArgumentException(
                    "service with id '%s' already exists".formatted(service.getId()));
        }

        validate(service);
        resolve(service);
        WorkspaceInfo workspace = service.getWorkspace();
        if (workspace != null
                && facade.getServiceByName(service.getName(), workspace, ServiceInfo.class)
                        != null) {
            throw new IllegalArgumentException(
                    "service with name '%s' already exists in workspace '%s'"
                            .formatted(service.getName(), workspace.getName()));
        }
        facade.add(service);

        // fire post modification event
        firePostServiceModified(service);
    }

    void resolve(ServiceInfo service) {
        resolveCollections(service);
    }

    public static <T> T unwrap(T obj) {
        return RepositoryGeoServerFacadeImpl.unwrap(obj);
    }

    @Override
    public <T extends ServiceInfo> T getService(Class<T> clazz) {
        WorkspaceInfo ws = LocalWorkspace.get();
        T service = ws != null ? facade.getService(ws, clazz) : null;
        service = service != null ? service : facade.getService(clazz);
        if (service == null) {
            LOGGER.severe(
                    () ->
                            "Could not locate service of type %s, local workspace is %s"
                                    .formatted(clazz, ws));
        }

        return service;
    }

    @Override
    public <T extends ServiceInfo> T getService(WorkspaceInfo workspace, Class<T> clazz) {
        return facade.getService(workspace, clazz);
    }

    @Override
    public <T extends ServiceInfo> T getService(String id, Class<T> clazz) {
        return facade.getService(id, clazz);
    }

    @Override
    public <T extends ServiceInfo> T getServiceByName(String name, Class<T> clazz) {
        T service =
                LocalWorkspace.get() != null
                        ? facade.getServiceByName(name, LocalWorkspace.get(), clazz)
                        : null;
        return service != null ? service : facade.getServiceByName(name, clazz);
    }

    @Override
    public <T extends ServiceInfo> T getServiceByName(
            WorkspaceInfo workspace, String name, Class<T> clazz) {
        return facade.getServiceByName(name, workspace, clazz);
    }

    @Override
    public Collection<? extends ServiceInfo> getServices() {
        WorkspaceInfo localWorkspace = LocalWorkspace.get();
        Collection<? extends ServiceInfo> services;
        if (localWorkspace == null) {
            services = facade.getServices();
        } else {
            services = facade.getServices(localWorkspace);
        }
        return services;
    }

    @Override
    public Collection<? extends ServiceInfo> getServices(WorkspaceInfo workspace) {
        return facade.getServices(workspace);
    }

    @Override
    public void remove(ServiceInfo service) {
        facade.remove(service);

        fireServiceRemoved(service);
    }

    @Override
    public void save(GeoServerInfo geoServer) {
        facade.save(geoServer);

        // fire post modification event
        fireGlobalPostModified();
    }

    @Override
    public void save(LoggingInfo logging) {
        facade.save(logging);

        // fire post modification event
        fireLoggingPostModified();
    }

    protected void fireGlobalPostModified() {
        GeoServerInfo global = getGlobal();
        listeners.forEach(l -> notifyPost(l, global));
    }

    private void notifyPost(ConfigurationListener l, GeoServerInfo global) {
        try {
            l.handlePostGlobalChange(global);
        } catch (RuntimeException e) {
            logListenerError(e);
        }
    }

    @Override
    public void fireGlobalModified(
            GeoServerInfo global,
            List<String> changed,
            List<Object> oldValues,
            List<Object> newValues) {

        for (ConfigurationListener l : getListeners()) {
            try {
                l.handleGlobalChange(global, changed, oldValues, newValues);
            } catch (RuntimeException e) {
                logListenerError(e);
            }
        }
    }

    @Override
    public void fireLoggingModified(
            LoggingInfo logging,
            List<String> changed,
            List<Object> oldValues,
            List<Object> newValues) {

        for (ConfigurationListener l : getListeners()) {
            try {
                l.handleLoggingChange(logging, changed, oldValues, newValues);
            } catch (Exception e) {
                logListenerError(e);
            }
        }
    }

    void fireLoggingPostModified() {
        LoggingInfo logging = getLogging();
        listeners.forEach(l -> notifyPost(logging, l));
    }

    protected void notifyPost(LoggingInfo logging, ConfigurationListener l) {
        try {
            l.handlePostLoggingChange(logging);
        } catch (Exception e) {
            logListenerError(e);
        }
    }

    @Override
    public void save(ServiceInfo service) {
        validate(service);

        facade.save(service);

        // fire post modification event
        firePostServiceModified(service);
    }

    void validate(ServiceInfo service) {
        CatalogImpl.validateKeywords(service.getKeywords());
        Objects.requireNonNull(service.getName(), "service name can't be null");
    }

    @Override
    public void fireServiceModified(
            ServiceInfo service,
            List<String> changed,
            List<Object> oldValues,
            List<Object> newValues) {

        for (ConfigurationListener l : getListeners()) {
            try {
                l.handleServiceChange(service, changed, oldValues, newValues);
            } catch (Exception e) {
                logListenerError(e);
            }
        }
    }

    void firePostServiceModified(ServiceInfo service) {
        for (ConfigurationListener l : listeners) {
            try {
                l.handlePostServiceChange(service);
            } catch (Exception e) {
                logListenerError(e);
            }
        }
    }

    void fireServiceRemoved(ServiceInfo service) {
        for (ConfigurationListener l : getListeners()) {
            try {
                l.handleServiceRemove(service);
            } catch (Exception e) {
                logListenerError(e);
            }
        }
    }

    void fireBeforeReload(List<GeoServerLifecycleHandler> handlers) {
        for (GeoServerLifecycleHandler handler : handlers) {
            try {
                handler.beforeReload();
            } catch (RuntimeException t) {
                LOGGER.log(
                        Level.SEVERE,
                        "A GeoServer lifecycle handler threw an exception during reload",
                        t);
            }
        }
    }

    void fireOnReload(List<GeoServerLifecycleHandler> handlers) {
        for (GeoServerLifecycleHandler handler : handlers) {
            try {
                handler.onReload();
            } catch (Exception t) {
                LOGGER.log(
                        Level.SEVERE,
                        "A GeoServer lifecycle handler threw an exception during reload",
                        t);
            }
        }
    }

    void fireOnDispose(List<GeoServerLifecycleHandler> handlers) {
        for (GeoServerLifecycleHandler handler : handlers) {
            try {
                handler.onDispose();
            } catch (RuntimeException t) {
                LOGGER.log(
                        Level.SEVERE,
                        "A GeoServer lifecycle handler threw an exception during dispose",
                        t);
            }
        }
    }

    void fireOnReset(List<GeoServerLifecycleHandler> handlers) {
        for (GeoServerLifecycleHandler handler : handlers) {
            try {
                handler.onReset();
            } catch (RuntimeException t) {
                LOGGER.log(
                        Level.SEVERE,
                        "A GeoServer lifecycle handler threw an exception during reset",
                        t);
            }
        }
    }

    @Override
    public void addListener(ConfigurationListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ConfigurationListener listener) {
        listeners.remove(listener);
    }

    @Override
    public Collection<ConfigurationListener> getListeners() {
        return listeners;
    }

    @Override
    public void dispose() {
        dispose(false);
    }

    public void dispose(boolean silent) {
        // look for pluggable handlers
        if (!silent) {
            List<GeoServerLifecycleHandler> handlers =
                    GeoServerExtensions.extensions(GeoServerLifecycleHandler.class);
            fireOnDispose(handlers);
        } else {
            LOGGER.log(
                    Level.FINE,
                    "Reloading GeoServer configuration, but not notifying lifecycle dispose handlers");
        }

        // internal cleanup

        if (catalog != null) catalog.dispose();
        if (facade != null) facade.dispose();
    }

    @Override
    public void reload() throws Exception {
        this.reload(null, false);
    }

    @Override
    public void reload(Catalog newCatalog) throws Exception {
        this.reload(newCatalog, false);
    }

    public void reload(Catalog newCatalog, boolean silent) throws Exception {
        // notify start of reload
        List<GeoServerLifecycleHandler> handlers =
                GeoServerExtensions.extensions(GeoServerLifecycleHandler.class);
        if (!silent) {
            fireBeforeReload(handlers);
        } else {
            LOGGER.log(
                    Level.FINE,
                    "Reloading GeoServer configuration, but not notifying lifecycle beforeReload handlers");
        }

        // perform the reload
        try {
            // flush caches
            reset(silent);

            // reload configuration
            synchronized (org.geoserver.config.GeoServer.CONFIGURATION_LOCK) {
                getCatalog().getResourcePool().dispose();

                if (newCatalog != null) {
                    dispose(silent);

                    // reload catalog, make sure we reload the underlying catalog, not any wrappers
                    Catalog catalog = getCatalog();
                    if (catalog instanceof Wrapper catalogWrapper) {
                        catalog = catalogWrapper.unwrap(Catalog.class);
                    }

                    ((CatalogImpl) catalog).sync((CatalogImpl) newCatalog);
                    ((CatalogImpl) catalog).resolve();
                } else {
                    callGeoServerLoaderReload();
                }
            }
        } finally {
            // notify end of reload
            if (!silent) {
                fireOnReload(handlers);
            } else {
                LOGGER.log(
                        Level.FINE,
                        "Reloading GeoServer configuration, but not notifying lifecycle onReload handlers");
            }
        }
    }

    private void callGeoServerLoaderReload() throws Exception {
        GeoServerLoaderProxy loaderProxy = GeoServerExtensions.bean(GeoServerLoaderProxy.class);
        if (loaderProxy == null) {
            GeoServerLoader geoServerLoader = GeoServerExtensions.bean(GeoServerLoader.class);
            geoServerLoader.reload();
        } else {
            loaderProxy.reload();
        }
    }

    @Override
    public void reset() {
        this.reset(false);
    }

    public void reset(boolean silent) {
        // drop all the catalog store/feature types/raster caches
        catalog.getResourcePool().dispose();

        // reset the referencing subsystem
        CRS.reset("all");

        // look for pluggable handlers
        if (!silent) {
            List<GeoServerLifecycleHandler> handlers =
                    GeoServerExtensions.extensions(GeoServerLifecycleHandler.class);
            fireOnReset(handlers);
        } else {
            LOGGER.log(
                    Level.FINE,
                    "Reloading GeoServer configuration, but not notifying lifecycle onReset handlers");
        }
    }

    private void logListenerError(Exception e) {
        LOGGER.log(Level.SEVERE, "Error occurred processing a configuration change listener", e);
    }
}
