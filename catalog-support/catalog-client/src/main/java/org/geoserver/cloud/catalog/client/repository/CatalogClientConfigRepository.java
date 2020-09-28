/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveConfigClient;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.plugin.ConfigRepository;

/** */
public class CatalogClientConfigRepository implements ConfigRepository {

    private @Getter @Setter ReactiveConfigClient client;

    public CatalogClientConfigRepository() {}

    public CatalogClientConfigRepository(ReactiveConfigClient configClient) {
        this.client = configClient;
    }

    public @Override Optional<GeoServerInfo> getGlobal() {
        return client.getGlobal().blockOptional();
    }

    public @Override void setGlobal(GeoServerInfo global) {
        checkNotAProxy(global);
        client.setGlobal(global).block();
    }

    public @Override Optional<SettingsInfo> getSettingsByWorkspace(WorkspaceInfo workspace) {
        return client.getSettingsByWorkspace(workspace.getId()).blockOptional();
    }

    public @Override void add(SettingsInfo settings) {
        checkNotAProxy(settings);
        client.createSettings(settings.getWorkspace().getId(), settings).block();
    }

    public @Override SettingsInfo update(SettingsInfo settings, Patch patch) {
        return client.updateSettings(settings.getWorkspace().getId(), patch).block();
    }

    public @Override void remove(SettingsInfo settings) {
        client.deleteSettings(settings.getWorkspace().getId()).block();
    }

    public @Override Optional<LoggingInfo> getLogging() {
        return client.getLogging().blockOptional();
    }

    public @Override void setLogging(LoggingInfo logging) {
        checkNotAProxy(logging);
        client.setLogging(logging).block();
    }

    public @Override void add(ServiceInfo service) {
        checkNotAProxy(service);
        WorkspaceInfo workspace = service.getWorkspace();
        if (workspace == null) {
            client.createService(service).block();
        } else {
            client.createService(workspace.getId(), service).block();
        }
    }

    public @Override void remove(ServiceInfo service) {
        client.deleteService(service.getId());
    }

    @SuppressWarnings("unchecked")
    public @Override <S extends ServiceInfo> S update(S service, Patch patch) {
        return (S) client.updateService(service.getId(), patch).block();
    }

    public @Override Stream<? extends ServiceInfo> getGlobalServices() {
        return client.getGlobalServices().toStream();
    }

    public @Override Stream<? extends ServiceInfo> getServicesByWorkspace(WorkspaceInfo workspace) {
        return client.getServicesByWorkspace(workspace.getId()).toStream();
    }

    public @Override <T extends ServiceInfo> Optional<T> getGlobalService(Class<T> clazz) {
        String typeName = interfaceName(clazz);
        return client.getGlobalServiceByType(typeName).blockOptional().map(clazz::cast);
    }

    public @Override <T extends ServiceInfo> Optional<T> getServiceByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {
        String typeName = interfaceName(clazz);
        return client.getServiceByWorkspaceAndType(workspace.getId(), typeName)
                .blockOptional()
                .map(clazz::cast);
    }

    public @Override <T extends ServiceInfo> Optional<T> getServiceById(String id, Class<T> clazz) {
        return client.getServiceById(id).blockOptional().filter(clazz::isInstance).map(clazz::cast);
    }

    public @Override <T extends ServiceInfo> Optional<T> getServiceByName(
            String name, Class<T> clazz) {
        return client.getGlobalServiceByName(name)
                .blockOptional()
                .filter(clazz::isInstance)
                .map(clazz::cast);
    }

    public @Override <T extends ServiceInfo> Optional<T> getServiceByNameAndWorkspace(
            String name, WorkspaceInfo workspace, Class<T> clazz) {
        return client.getServiceByWorkspaceAndName(workspace.getId(), name)
                .blockOptional()
                .filter(clazz::isInstance)
                .map(clazz::cast);
    }

    /** no-op */
    public @Override void dispose() {
        // no-op
    }

    protected <T extends ServiceInfo> String interfaceName(Class<T> clazz) {
        if (!clazz.isInterface())
            throw new IllegalArgumentException("Expected interface type, got " + clazz);
        String typeName = clazz.getName();
        return typeName;
    }

    private static void checkNotAProxy(Info value) {
        if (Proxy.isProxyClass(value.getClass())) {
            throw new IllegalArgumentException(
                    "Proxy values shall not be passed to DefaultConfigRepository");
        }
    }
}
