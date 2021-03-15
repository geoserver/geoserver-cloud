/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.config.plugin.forwarding;

import java.util.Collection;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.plugin.ConfigRepository;
import org.geoserver.config.plugin.RepositoryGeoServerFacade;

/** */
public class ForwardingGeoServerFacade implements RepositoryGeoServerFacade {

    private GeoServerFacade facade;

    public ForwardingGeoServerFacade(GeoServerFacade facade) {
        this.facade = facade;
    }

    /** @return this decorator's subject */
    public GeoServerFacade getSubject() {
        // if you're wondering, I refuse to derive from org.geotools.util.decorate.AbstractDecorator
        // and by extension from java.sql.Wrapper
        return facade;
    }

    public @Override void setRepository(ConfigRepository repository) {
        if (facade instanceof RepositoryGeoServerFacade)
            ((RepositoryGeoServerFacade) facade).setRepository(repository);

        throw new IllegalStateException(
                "subject GeoServerFacade is not a RepositoryGeoServerFacade");
    }

    public @Override GeoServer getGeoServer() {
        return facade.getGeoServer();
    }

    public @Override void setGeoServer(GeoServer geoServer) {
        facade.setGeoServer(geoServer);
    }

    public @Override GeoServerInfo getGlobal() {
        return facade.getGlobal();
    }

    public @Override void setGlobal(GeoServerInfo global) {
        facade.setGlobal(global);
    }

    public @Override void save(GeoServerInfo geoServer) {
        facade.save(geoServer);
    }

    public @Override SettingsInfo getSettings(WorkspaceInfo workspace) {
        return facade.getSettings(workspace);
    }

    public @Override void add(SettingsInfo settings) {
        facade.add(settings);
    }

    public @Override void save(SettingsInfo settings) {
        facade.save(settings);
    }

    public @Override void remove(SettingsInfo settings) {
        facade.remove(settings);
    }

    public @Override LoggingInfo getLogging() {
        return facade.getLogging();
    }

    public @Override void setLogging(LoggingInfo logging) {
        facade.setLogging(logging);
    }

    public @Override void save(LoggingInfo logging) {
        facade.save(logging);
    }

    public @Override void add(ServiceInfo service) {
        facade.add(service);
    }

    public @Override void remove(ServiceInfo service) {
        facade.remove(service);
    }

    public @Override void save(ServiceInfo service) {
        facade.save(service);
    }

    public @Override Collection<? extends ServiceInfo> getServices() {
        return facade.getServices();
    }

    public @Override Collection<? extends ServiceInfo> getServices(WorkspaceInfo workspace) {
        return facade.getServices(workspace);
    }

    public @Override <T extends ServiceInfo> T getService(Class<T> clazz) {
        return facade.getService(clazz);
    }

    public @Override <T extends ServiceInfo> T getService(WorkspaceInfo workspace, Class<T> clazz) {
        return facade.getService(workspace, clazz);
    }

    public @Override <T extends ServiceInfo> T getService(String id, Class<T> clazz) {
        return facade.getService(id, clazz);
    }

    public @Override <T extends ServiceInfo> T getServiceByName(String name, Class<T> clazz) {
        return facade.getServiceByName(name, clazz);
    }

    public @Override <T extends ServiceInfo> T getServiceByName(
            String name, WorkspaceInfo workspace, Class<T> clazz) {
        return facade.getServiceByName(name, workspace, clazz);
    }

    public @Override void dispose() {
        facade.dispose();
    }

    public @Override SettingsInfo getSettings(String id) {
        if (facade instanceof RepositoryGeoServerFacade)
            ((RepositoryGeoServerFacade) facade).getSettings(id);

        throw new UnsupportedOperationException();
    }
}
