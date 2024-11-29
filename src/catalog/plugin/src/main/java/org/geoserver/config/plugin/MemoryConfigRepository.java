/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.config.plugin;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

/**
 * Purely in-memory {@link ConfigRepository} implementation holding live-objects (no serialization
 * nor {@link Proxy proxying} involved)
 */
public class MemoryConfigRepository implements ConfigRepository {

    protected GeoServerInfo global;
    protected LoggingInfo logging;
    protected final ConcurrentMap<String, SettingsInfo> settings = new ConcurrentHashMap<>();
    protected final ConcurrentMap<String, ServiceInfo> services = new ConcurrentHashMap<>();

    @Override
    public Optional<GeoServerInfo> getGlobal() {
        return Optional.ofNullable(global);
    }

    private static void checkNotAProxy(Info value) {
        if (Proxy.isProxyClass(value.getClass())) {
            throw new IllegalArgumentException("Proxy values shall not be passed to DefaultConfigRepository");
        }
    }

    @Override
    public void setGlobal(GeoServerInfo global) {
        requireNonNull(global);
        checkNotAProxy(global);
        this.global = global;
    }

    @Override
    public Optional<SettingsInfo> getSettingsById(String id) {
        return Optional.ofNullable(settings.get(id));
    }

    @Override
    public Optional<SettingsInfo> getSettingsByWorkspace(WorkspaceInfo workspace) {
        requireNonNull(workspace);
        requireNonNull(workspace.getId());
        return settings.values().stream()
                .filter(s -> s.getWorkspace().getId().equals(workspace.getId()))
                .findFirst();
    }

    @Override
    public void add(SettingsInfo settings) {
        requireNonNull(settings);
        requireNonNull(settings.getId());
        checkNotAProxy(settings);
        this.settings.put(settings.getId(), settings);
    }

    @Override
    public SettingsInfo update(SettingsInfo settings, Patch patch) {
        requireNonNull(settings);
        requireNonNull(patch);
        requireNonNull(settings.getId());
        requireNonNull(settings.getWorkspace());
        checkNotAProxy(settings);

        SettingsInfo localCopy = this.settings.get(settings.getId());
        synchronized (localCopy) {
            patch.applyTo(localCopy, SettingsInfo.class);
        }
        return localCopy;
    }

    @Override
    public void remove(SettingsInfo settings) {
        requireNonNull(settings);
        requireNonNull(settings.getId());
        this.settings.remove(settings.getId());
    }

    @Override
    public Optional<LoggingInfo> getLogging() {
        return Optional.ofNullable(logging);
    }

    @Override
    public void setLogging(LoggingInfo logging) {
        requireNonNull(logging);
        checkNotAProxy(logging);
        this.logging = logging;
    }

    @Override
    public void add(ServiceInfo service) {
        requireNonNull(service);
        requireNonNull(service.getId());
        checkNotAProxy(service);

        this.services.put(service.getId(), service);
    }

    @Override
    public void remove(ServiceInfo service) {
        requireNonNull(service);
        requireNonNull(service.getId());
        this.services.remove(service.getId());
    }

    @Override
    public <S extends ServiceInfo> S update(S service, Patch patch) {
        requireNonNull(service);
        requireNonNull(service.getId());
        checkNotAProxy(service);
        requireNonNull(patch);

        @SuppressWarnings("unchecked")
        S localCopy = (S) this.services.get(service.getId());
        synchronized (localCopy) {
            patch.applyTo(localCopy);
        }
        return localCopy;
    }

    @Override
    public Stream<ServiceInfo> getGlobalServices() {
        return this.services.values().stream().filter(s -> s.getWorkspace() == null);
    }

    @Override
    public Stream<ServiceInfo> getServicesByWorkspace(WorkspaceInfo workspace) {
        requireNonNull(workspace);
        requireNonNull(workspace.getId());
        return this.services.values().stream()
                .filter(s -> s.getWorkspace() != null
                        && workspace.getId().equals(s.getWorkspace().getId()));
    }

    @Override
    public <T extends ServiceInfo> Optional<T> getGlobalService(Class<T> clazz) {
        requireNonNull(clazz);
        return this.services.values().stream()
                .filter(clazz::isInstance)
                .filter(s -> s.getWorkspace() == null)
                .map(clazz::cast)
                .findFirst();
    }

    @Override
    public <T extends ServiceInfo> Optional<T> getServiceByWorkspace(WorkspaceInfo workspace, Class<T> clazz) {
        requireNonNull(workspace);
        requireNonNull(workspace.getId());
        requireNonNull(clazz);
        return this.services.values().stream()
                .filter(clazz::isInstance)
                .filter(s ->
                        s.getWorkspace() != null && s.getWorkspace().getId().equals(workspace.getId()))
                .map(clazz::cast)
                .findFirst();
    }

    @Override
    public <T extends ServiceInfo> Optional<T> getServiceById(String id, Class<T> clazz) {
        requireNonNull(id);
        requireNonNull(clazz);
        ServiceInfo service = services.get(id);
        return clazz.isInstance(service) ? Optional.of(clazz.cast(service)) : Optional.empty();
    }

    @Override
    public <T extends ServiceInfo> Optional<T> getServiceByName(String name, Class<T> clazz) {
        requireNonNull(name);
        requireNonNull(clazz);
        return this.services.values().stream()
                .filter(clazz::isInstance)
                .filter(s -> name.equals(s.getName()))
                .map(clazz::cast)
                .findFirst();
    }

    @Override
    public <T extends ServiceInfo> Optional<T> getServiceByNameAndWorkspace(
            String name, WorkspaceInfo workspace, Class<T> clazz) {
        requireNonNull(name);
        requireNonNull(workspace);
        requireNonNull(workspace.getId());
        requireNonNull(clazz);
        return this.services.values().stream()
                .filter(clazz::isInstance)
                .filter(s -> s.getWorkspace() != null
                        && s.getWorkspace().getId().equals(workspace.getId())
                        && name.equals(s.getName()))
                .map(clazz::cast)
                .findFirst();
    }

    @Override
    public void dispose() {
        global = null;
        logging = null;
        settings.clear();
        services.clear();
    }
}
