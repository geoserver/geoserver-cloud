/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

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

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/** */
public class CatalogClientConfigRepository implements ConfigRepository {

    private @Getter @Setter ReactiveConfigClient client;

    public CatalogClientConfigRepository() {}

    public CatalogClientConfigRepository(ReactiveConfigClient configClient) {
        this.client = configClient;
    }

    protected void block(Mono<Void> call) {
        if (Schedulers.isInNonBlockingThread()) {
            CompletableFuture.supplyAsync(call::block).join();
        }
        call.block();
    }

    protected <U> Optional<U> blockAndReturn(Mono<U> call) {
        if (Schedulers.isInNonBlockingThread()) {
            return CompletableFuture.supplyAsync(call::blockOptional).join();
        }
        return call.blockOptional();
    }

    public @Override Optional<GeoServerInfo> getGlobal() {
        return blockAndReturn(client.getGlobal());
    }

    public @Override void setGlobal(GeoServerInfo global) {
        checkNotAProxy(global);
        blockAndReturn(client.setGlobal(global));
    }

    public @Override Optional<SettingsInfo> getSettingsById(String id) {
        return blockAndReturn(client.getSettingsById(id));
    }

    public @Override Optional<SettingsInfo> getSettingsByWorkspace(WorkspaceInfo workspace) {
        return blockAndReturn(client.getSettingsByWorkspace(workspace.getId()));
    }

    public @Override void add(SettingsInfo settings) {
        checkNotAProxy(settings);
        blockAndReturn(client.createSettings(settings.getWorkspace().getId(), settings));
    }

    public @Override SettingsInfo update(SettingsInfo settings, Patch patch) {
        return blockAndReturn(client.updateSettings(settings.getWorkspace().getId(), patch))
                .orElseThrow(() -> new IllegalStateException());
    }

    public @Override void remove(SettingsInfo settings) {
        blockAndReturn(client.deleteSettings(settings.getWorkspace().getId()));
    }

    public @Override Optional<LoggingInfo> getLogging() {
        return blockAndReturn(client.getLogging());
    }

    public @Override void setLogging(LoggingInfo logging) {
        checkNotAProxy(logging);
        blockAndReturn(client.setLogging(logging));
    }

    public @Override void add(ServiceInfo service) {
        checkNotAProxy(service);
        WorkspaceInfo workspace = service.getWorkspace();
        if (workspace == null) {
            blockAndReturn(client.createService(service));
        } else {
            blockAndReturn(client.createService(workspace.getId(), service));
        }
    }

    public @Override void remove(ServiceInfo service) {
        client.deleteService(service.getId());
    }

    @SuppressWarnings("unchecked")
    public @Override <S extends ServiceInfo> S update(S service, Patch patch) {
        Optional<ServiceInfo> updated =
                blockAndReturn(client.updateService(service.getId(), patch));
        return updated.map(u -> (S) u).orElseThrow(() -> new IllegalStateException());
    }

    public @Override Stream<? extends ServiceInfo> getGlobalServices() {
        return client.getGlobalServices().toStream();
    }

    public @Override Stream<? extends ServiceInfo> getServicesByWorkspace(WorkspaceInfo workspace) {
        return client.getServicesByWorkspace(workspace.getId()).toStream();
    }

    public @Override <T extends ServiceInfo> Optional<T> getGlobalService(Class<T> clazz) {
        String typeName = interfaceName(clazz);
        return blockAndReturn(client.getGlobalServiceByType(typeName).map(clazz::cast));
    }

    public @Override <T extends ServiceInfo> Optional<T> getServiceByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {
        String typeName = interfaceName(clazz);
        return blockAndReturn(
                client.getServiceByWorkspaceAndType(workspace.getId(), typeName).map(clazz::cast));
    }

    public @Override <T extends ServiceInfo> Optional<T> getServiceById(String id, Class<T> clazz) {
        return blockAndReturn(client.getServiceById(id).filter(clazz::isInstance).map(clazz::cast));
    }

    public @Override <T extends ServiceInfo> Optional<T> getServiceByName(
            String name, Class<T> clazz) {
        return blockAndReturn(
                client.getGlobalServiceByName(name).filter(clazz::isInstance).map(clazz::cast));
    }

    public @Override <T extends ServiceInfo> Optional<T> getServiceByNameAndWorkspace(
            String name, WorkspaceInfo workspace, Class<T> clazz) {
        return blockAndReturn(
                client.getServiceByWorkspaceAndName(workspace.getId(), name)
                        .filter(clazz::isInstance)
                        .map(clazz::cast));
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
