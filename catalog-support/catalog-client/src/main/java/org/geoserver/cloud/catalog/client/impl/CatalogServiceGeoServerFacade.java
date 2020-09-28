/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.impl;

import java.util.Collection;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveConfigClient;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

public class CatalogServiceGeoServerFacade implements GeoServerFacade {

    private @Getter @Setter GeoServer geoServer;

    private @Getter @Setter ReactiveConfigClient client;

    public @Override GeoServerInfo getGlobal() {
        return client.getGlobal().block();
    }

    public @Override void setGlobal(GeoServerInfo global) {
        client.setGlobal(global).block();
    }

    public @Override void save(GeoServerInfo global) {
        client.setGlobal(global).block();
    }

    public @Override SettingsInfo getSettings(WorkspaceInfo workspace) {
        return client.getSettingsByWorkspace(workspace.getId()).block();
    }

    public @Override void add(SettingsInfo settings) {
        client.createSettings(settings.getWorkspace().getId(), settings).block();
    }

    public @Override void save(SettingsInfo settings) {
        client.saveSettings(settings.getWorkspace().getId(), settings).block();
    }

    public @Override void remove(SettingsInfo settings) {
        client.deleteSettings(settings.getWorkspace().getId()).block();
    }

    public @Override LoggingInfo getLogging() {
        return client.getLogging().block();
    }

    public @Override void setLogging(LoggingInfo logging) {
        client.setLogging(logging).block();
    }

    public @Override void save(LoggingInfo logging) {
        client.setLogging(logging).block();
    }

    public @Override void add(ServiceInfo service) {
        WorkspaceInfo workspace = service.getWorkspace();
        if (workspace == null) client.createService(service).block();
        else client.createService(workspace.getId(), service).block();
    }

    public @Override void remove(ServiceInfo service) {
        client.deleteService(service.getId()).block();
    }

    public @Override void save(ServiceInfo service) {
        if (service.getWorkspace() == null) client.updateService(service).block();
        else client.updateService(service.getWorkspace().getId(), service).block();
    }

    public @Override Collection<? extends ServiceInfo> getServices() {
        return client.getGlobalServices().toStream().collect(Collectors.toList());
    }

    public @Override Collection<? extends ServiceInfo> getServices(WorkspaceInfo workspace) {
        return client.getServicesByWorkspace(workspace.getId())
                .toStream()
                .collect(Collectors.toList());
    }

    public @Override <T extends ServiceInfo> T getService(Class<T> clazz) {
        return client.getGlobalService(clazz).block();
    }

    public @Override <T extends ServiceInfo> T getService(WorkspaceInfo workspace, Class<T> clazz) {
        return client.getServiceByWorkspaceAndType(workspace.getId(), clazz).block();
    }

    public @Override <T extends ServiceInfo> T getService(String id, Class<T> clazz) {
        ServiceInfo service = client.getServiceById(id).block();
        return clazz.isInstance(service) ? clazz.cast(service) : null;
    }

    public @Override <T extends ServiceInfo> T getServiceByName(String name, Class<T> clazz) {
        ServiceInfo service = client.getServiceByName(name).block();
        return clazz.isInstance(service) ? clazz.cast(service) : null;
    }

    public @Override <T extends ServiceInfo> T getServiceByName(
            String name, WorkspaceInfo workspace, Class<T> clazz) {
        ServiceInfo service = client.getServiceByWorkspaceAndName(workspace.getId(), name).block();
        return clazz.isInstance(service) ? clazz.cast(service) : null;
    }

    /** no-op */
    public @Override void dispose() {}
}
