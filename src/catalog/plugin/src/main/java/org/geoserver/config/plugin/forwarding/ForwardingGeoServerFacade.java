/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.config.plugin.forwarding;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.plugin.ConfigRepository;
import org.geoserver.config.plugin.RepositoryGeoServerFacade;

import java.util.Collection;

/** */
public class ForwardingGeoServerFacade implements RepositoryGeoServerFacade {

    private GeoServerFacade facade;

    public ForwardingGeoServerFacade(GeoServerFacade facade) {
        this.facade = facade;
    }

    /**
     * @return this decorator's subject
     */
    public GeoServerFacade getSubject() {
        // if you're wondering, I refuse to derive from org.geotools.util.decorate.AbstractDecorator
        // and by extension from java.sql.Wrapper
        return facade;
    }

    @Override
    public void setRepository(ConfigRepository repository) {
        if (facade instanceof RepositoryGeoServerFacade repoFacade)
            repoFacade.setRepository(repository);

        throw new IllegalStateException(
                "subject GeoServerFacade is not a RepositoryGeoServerFacade");
    }

    @Override
    public GeoServer getGeoServer() {
        return facade.getGeoServer();
    }

    @Override
    public void setGeoServer(GeoServer geoServer) {
        facade.setGeoServer(geoServer);
    }

    @Override
    public GeoServerInfo getGlobal() {
        return facade.getGlobal();
    }

    @Override
    public void setGlobal(GeoServerInfo global) {
        facade.setGlobal(global);
    }

    @Override
    public void save(GeoServerInfo geoServer) {
        facade.save(geoServer);
    }

    @Override
    public SettingsInfo getSettings(WorkspaceInfo workspace) {
        return facade.getSettings(workspace);
    }

    @Override
    public void add(SettingsInfo settings) {
        facade.add(settings);
    }

    @Override
    public void save(SettingsInfo settings) {
        facade.save(settings);
    }

    @Override
    public void remove(SettingsInfo settings) {
        facade.remove(settings);
    }

    @Override
    public LoggingInfo getLogging() {
        return facade.getLogging();
    }

    @Override
    public void setLogging(LoggingInfo logging) {
        facade.setLogging(logging);
    }

    @Override
    public void save(LoggingInfo logging) {
        facade.save(logging);
    }

    @Override
    public void add(ServiceInfo service) {
        facade.add(service);
    }

    @Override
    public void remove(ServiceInfo service) {
        facade.remove(service);
    }

    @Override
    public void save(ServiceInfo service) {
        facade.save(service);
    }

    @Override
    public Collection<? extends ServiceInfo> getServices() {
        return facade.getServices();
    }

    @Override
    public Collection<? extends ServiceInfo> getServices(WorkspaceInfo workspace) {
        return facade.getServices(workspace);
    }

    @Override
    public <T extends ServiceInfo> T getService(Class<T> clazz) {
        return facade.getService(clazz);
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
        return facade.getServiceByName(name, clazz);
    }

    @Override
    public <T extends ServiceInfo> T getServiceByName(
            String name, WorkspaceInfo workspace, Class<T> clazz) {
        return facade.getServiceByName(name, workspace, clazz);
    }

    @Override
    public void dispose() {
        facade.dispose();
    }

    @Override
    public SettingsInfo getSettings(String id) {
        if (facade instanceof RepositoryGeoServerFacade repoFacade) repoFacade.getSettings(id);

        throw new UnsupportedOperationException();
    }
}
