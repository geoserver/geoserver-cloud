/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.jdbcconfig;

import static org.geoserver.catalog.CatalogFacade.ANY_WORKSPACE;
import static org.geoserver.catalog.Predicates.and;
import static org.geoserver.catalog.Predicates.equal;
import static org.geoserver.catalog.Predicates.isNull;

import com.google.common.base.Preconditions;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.jdbcconfig.config.JDBCGeoServerFacade;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.api.filter.Filter;
import org.geotools.util.logging.Logging;

import java.lang.reflect.Proxy;
import java.rmi.server.UID;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Copy of {@link JDBCGeoServerFacade} that does not try reinitialize logging, can't extend it
 * 'cause the field "geoServer" is private and need to override {@link #setGeoServer(GeoServer)}
 */
public class CloudJdbcGeoserverFacade implements GeoServerFacade {

    static final Logger LOGGER = Logging.getLogger(JDBCGeoServerFacade.class);

    private static final String GLOBAL_ID = "GeoServerInfo.global";

    private static final String GLOBAL_LOGGING_ID = "LoggingInfo.global";

    private GeoServer geoServer;

    public final ConfigDatabase db;

    public CloudJdbcGeoserverFacade(final ConfigDatabase db) {
        this.db = db;
    }

    @Override
    public GeoServer getGeoServer() {
        return geoServer;
    }

    @Override
    public void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;
        this.db.setGeoServer(geoServer);
    }

    @Override
    public GeoServerInfo getGlobal() {
        GeoServerInfo global = db.getById(GLOBAL_ID, GeoServerInfo.class);
        return global;
    }

    @Override
    public void setGlobal(GeoServerInfo global) {
        OwsUtils.set(global, "id", GLOBAL_ID);
        if (global.getSettings() == null) {
            SettingsInfo defaultSettings = geoServer.getFactory().createSettings();
            add(defaultSettings);
            global.setSettings(defaultSettings);
            // JD: disabling this check, global settings should have an id
            // }else if(null == global.getSettings().getId()){
        } else {
            add(global.getSettings());
        }
        if (null == getGlobal()) {
            db.add(global);
        } else {
            db.save(ModificationProxy.create(global, GeoServerInfo.class));
        }
        GeoServerInfo saved = getGlobal();
        Preconditions.checkNotNull(saved);
    }

    @Override
    public void save(GeoServerInfo global) {
        // this object is a proxy
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(global);
        // fire out what changed
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();

        geoServer.fireGlobalModified(global, propertyNames, oldValues, newValues);

        db.save(global);
    }

    @Override
    public LoggingInfo getLogging() {
        LoggingInfo loggingInfo = db.getById(GLOBAL_LOGGING_ID, LoggingInfo.class);
        return loggingInfo;
    }

    @Override
    public void setLogging(LoggingInfo logging) {
        OwsUtils.set(logging, "id", GLOBAL_LOGGING_ID);
        if (null == getLogging()) {
            db.add(logging);
        } else {
            db.save(ModificationProxy.create(logging, LoggingInfo.class));
        }
        LoggingInfo saved = getLogging();
        Preconditions.checkNotNull(saved);
    }

    @Override
    public void save(LoggingInfo logging) {
        // this object is a proxy
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(logging);
        // fire out what changed
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();

        geoServer.fireLoggingModified(logging, propertyNames, oldValues, newValues);

        db.save(logging);
    }

    @Override
    public void add(ServiceInfo service) {
        setId(service, ServiceInfo.class);
        service.setGeoServer(geoServer);
        db.add(service);
    }

    @Override
    public void remove(ServiceInfo service) {
        db.remove(service);
    }

    @Override
    public void save(ServiceInfo service) {
        // this object is a proxy
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(service);
        // fire out what changed
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();

        geoServer.fireServiceModified(service, propertyNames, oldValues, newValues);

        db.save(service);
    }

    @Override
    public SettingsInfo getSettings(WorkspaceInfo workspace) {
        //        Filter filter = equal("workspace.id", workspace.getId());
        //        return db.get(SettingsInfo.class, filter);

        String wsId = workspace.getId();
        return db.getByIdentity(SettingsInfo.class, "workspace.id", wsId);
    }

    @Override
    public void add(SettingsInfo settings) {
        setId(settings, SettingsInfo.class);
        db.add(settings);
    }

    @Override
    public void save(SettingsInfo settings) {
        // this object is a proxy
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(settings);
        // fire out what changed
        List<String> propertyNames = h.getPropertyNames();
        List<Object> newValues = h.getNewValues();
        List<Object> oldValues = h.getOldValues();

        geoServer.fireSettingsModified(settings, propertyNames, oldValues, newValues);

        db.save(settings);
    }

    @Override
    public void remove(SettingsInfo settings) {
        db.remove(settings);
    }

    @Override
    public Collection<? extends ServiceInfo> getServices() {
        return getServices((WorkspaceInfo) null);
    }

    private Filter filterForWorkspace(WorkspaceInfo workspace) {
        if (workspace != null && workspace != ANY_WORKSPACE) {
            return equal("workspace.id", workspace.getId());
        } else {
            return filterForGlobal();
        }
    }

    private Filter filterForGlobal() {
        return isNull("workspace.id");
    }

    @Override
    public Collection<? extends ServiceInfo> getServices(WorkspaceInfo workspace) {

        Filter filter = filterForWorkspace(workspace);
        return db.queryAsList(ServiceInfo.class, filter, null, null, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ServiceInfo> T getService(final Class<T> clazz) {
        return (T) db.getService(null, clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ServiceInfo> T getService(
            final WorkspaceInfo workspace, final Class<T> clazz) {
        return (T) db.getService(workspace, clazz);
    }

    @Override
    public <T extends ServiceInfo> T getService(final String id, final Class<T> clazz) {
        return db.getById(id, clazz);
    }

    @Override
    public <T extends ServiceInfo> T getServiceByName(final String name, final Class<T> clazz) {
        return findByName(name, null, clazz);
    }

    @Override
    public <T extends ServiceInfo> T getServiceByName(
            final String name, final WorkspaceInfo workspace, final Class<T> clazz) {

        return findByName(name, workspace, clazz);
    }

    private <T extends Info> T findByName(
            @Nonnull final String name,
            @Nullable final WorkspaceInfo workspace,
            @Nonnull final Class<T> clazz)
            throws AssertionError {

        Filter filter = equal("name", name);
        if (null != workspace && ANY_WORKSPACE != workspace) {
            final String wsId = workspace.getId();
            Filter wsFilter = equal("workspace.id", wsId);
            filter = and(filter, wsFilter);
        }
        try {
            return db.get(clazz, filter);
        } catch (IllegalArgumentException multipleResults) {
            return null;
        }
    }

    @Override
    public void dispose() {
        db.dispose();
    }

    private void setId(Info info, Class<? extends Info> type) {
        final String curId = info.getId();
        if (null == curId) {
            final String uid = new UID().toString();
            final String id = type.getSimpleName() + "." + uid;
            OwsUtils.set(info, "id", id);
        }
    }
}
