/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.config.plugin;

import org.geoserver.catalog.Info;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.ows.util.OwsUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

/**
 * Example {@link ConfigRepository} alternative that serializes to XML using {@link
 * XStreamPersister} and keeps serialized config objects as strings in memory.
 */
class XmlSerializedConfigRepository implements ConfigRepository {

    protected String global;
    protected String logging;
    protected final ConcurrentMap<String, String> settings = new ConcurrentHashMap<>();
    protected final ConcurrentMap<String, String> services = new ConcurrentHashMap<>();

    private XStreamPersister codec;

    public XmlSerializedConfigRepository(XStreamPersister codec) {
        this.codec = codec;
    }

    private String serialize(Info info) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            codec.save(info, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private <I extends Info> I deserialize(String serialized, Class<I> type) {
        if (serialized == null) return null;
        I loaded;
        try {
            ByteArrayInputStream in =
                    new ByteArrayInputStream(serialized.getBytes(StandardCharsets.UTF_8));
            loaded = codec.load(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        OwsUtils.resolveCollections(loaded);
        return loaded;
    }

    private SettingsInfo toSettings(String serialized) {
        return deserialize(serialized, SettingsInfo.class);
    }

    private ServiceInfo toService(String serialized) {
        return deserialize(serialized, ServiceInfo.class);
    }

    @Override
    public Optional<GeoServerInfo> getGlobal() {
        return Optional.ofNullable(deserialize(global, GeoServerInfo.class));
    }

    private static void checkNotAProxy(Info value) {
        if (Proxy.isProxyClass(value.getClass())) {
            throw new IllegalArgumentException(
                    "Proxy values shall not be passed to DefaultConfigRepository");
        }
    }

    @Override
    public void setGlobal(GeoServerInfo global) {
        checkNotAProxy(global);
        this.global = serialize(global);
    }

    @Override
    public Optional<SettingsInfo> getSettingsById(String id) {
        return Optional.ofNullable(toSettings(settings.get(id)));
    }

    @Override
    public Optional<SettingsInfo> getSettingsByWorkspace(WorkspaceInfo workspace) {
        return settings.values().stream()
                .map(this::toSettings)
                .filter(s -> s.getWorkspace().getId().equals(workspace.getId()))
                .findFirst();
    }

    @Override
    public void add(SettingsInfo settings) {
        checkNotAProxy(settings);
        String serialized = serialize(settings);
        this.settings.put(settings.getId(), serialized);
    }

    @Override
    public SettingsInfo update(SettingsInfo settings, Patch patch) {
        checkNotAProxy(settings);

        String localCopy = this.settings.get(settings.getId());
        synchronized (localCopy) {
            SettingsInfo local = deserialize(localCopy, SettingsInfo.class);
            patch.applyTo(local);
            this.settings.put(settings.getId(), serialize(local));
            return local;
        }
    }

    @Override
    public void remove(SettingsInfo settings) {
        this.services.remove(settings.getId());
    }

    @Override
    public Optional<LoggingInfo> getLogging() {
        return Optional.ofNullable(deserialize(logging, LoggingInfo.class));
    }

    @Override
    public void setLogging(LoggingInfo logging) {
        this.logging = serialize(logging);
    }

    @Override
    public void add(ServiceInfo service) {
        checkNotAProxy(service);

        String serialized = serialize(service);
        this.services.put(service.getId(), serialized);
    }

    @Override
    public void remove(ServiceInfo service) {
        this.services.remove(service.getId());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S extends ServiceInfo> S update(S service, Patch patch) {
        checkNotAProxy(service);

        String localCopy = services.get(service.getId());
        synchronized (localCopy) {
            ServiceInfo local = deserialize(localCopy, service.getClass());
            patch.applyTo(local);
            this.services.put(service.getId(), serialize(local));
            return (S) local;
        }
    }

    @Override
    public Stream<ServiceInfo> getGlobalServices() {
        return services().filter(s -> s.getWorkspace() == null);
    }

    @Override
    public Stream<ServiceInfo> getServicesByWorkspace(WorkspaceInfo workspace) {
        return services()
                .filter(s -> s.getWorkspace() != null)
                .filter(s -> workspace.getId().equals(s.getWorkspace().getId()));
    }

    @Override
    public <T extends ServiceInfo> Optional<T> getGlobalService(Class<T> clazz) {
        return services()
                .filter(clazz::isInstance)
                .filter(s -> s.getWorkspace() == null)
                .map(clazz::cast)
                .findFirst();
    }

    @Override
    public <T extends ServiceInfo> Optional<T> getServiceByWorkspace(
            WorkspaceInfo workspace, Class<T> clazz) {

        return getServicesByWorkspace(workspace)
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .findFirst();
    }

    @Override
    public <T extends ServiceInfo> Optional<T> getServiceById(String id, Class<T> clazz) {

        ServiceInfo service = toService(services.get(id));
        return clazz.isInstance(service) ? Optional.of(clazz.cast(service)) : Optional.empty();
    }

    @Override
    public <T extends ServiceInfo> Optional<T> getServiceByName(String name, Class<T> clazz) {

        return getGlobalServices()
                .filter(clazz::isInstance)
                .filter(s -> name.equals(s.getName()))
                .map(clazz::cast)
                .findFirst();
    }

    @Override
    public <T extends ServiceInfo> Optional<T> getServiceByNameAndWorkspace(
            String name, WorkspaceInfo workspace, Class<T> clazz) {

        return getServicesByWorkspace(workspace)
                .filter(clazz::isInstance)
                .filter(s -> name.equals(s.getName()))
                .map(clazz::cast)
                .findFirst();
    }

    private Stream<ServiceInfo> services() {
        return this.services.values().stream().map(this::toService);
    }

    @Override
    public void dispose() {
        global = null;
        logging = null;
        settings.clear();
        services.clear();
    }
}
