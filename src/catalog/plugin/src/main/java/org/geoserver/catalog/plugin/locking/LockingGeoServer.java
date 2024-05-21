/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.locking;

import static org.geoserver.catalog.plugin.locking.LockingSupport.nameOf;
import static org.geoserver.catalog.plugin.locking.LockingSupport.typeOf;

import lombok.NonNull;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.plugin.GeoServerImpl;

/**
 * @since 1.0
 */
public class LockingGeoServer extends GeoServerImpl {

    private LockingSupport lockingSupport;
    private final GeoServerConfigurationLock configurationLock;

    public LockingGeoServer(@NonNull GeoServerConfigurationLock locking) {
        super();
        this.configurationLock = locking;
        enableLocking();
    }

    public LockingGeoServer(
            @NonNull GeoServerConfigurationLock locking, @NonNull GeoServerFacade facade) {
        super(facade);
        this.configurationLock = locking;
        enableLocking();
    }

    public void enableLocking() {
        this.lockingSupport = LockingSupport.locking(configurationLock);
    }

    public void disableLocking() {
        this.lockingSupport = LockingSupport.ignoringLocking();
    }

    public @NonNull GeoServerConfigurationLock getConfigurationLock() {
        return this.configurationLock;
    }

    @Override
    public void setGlobal(GeoServerInfo global) {
        lockingSupport.runInWriteLock(
                () -> super.setGlobal(global), "setGlobal(%s)".formatted(nameOf(global)));
    }

    @Override
    public void save(GeoServerInfo geoServer) {
        lockingSupport.runInWriteLock(
                () -> super.save(geoServer), "save(%s)".formatted(nameOf(geoServer)));
    }

    @Override
    public void add(SettingsInfo settings) {
        lockingSupport.runInWriteLock(
                () -> super.add(settings),
                "add(%s[%s])".formatted(typeOf(settings), nameOf(settings)));
    }

    @Override
    public void save(SettingsInfo settings) {
        lockingSupport.runInWriteLock(
                () -> super.save(settings),
                "save(%s[%s])".formatted(typeOf(settings), nameOf(settings)));
    }

    @Override
    public void remove(SettingsInfo settings) {
        lockingSupport.runInWriteLock(
                () -> super.remove(settings),
                "remove(%s[%s])".formatted(typeOf(settings), nameOf(settings)));
    }

    @Override
    public void setLogging(LoggingInfo logging) {
        lockingSupport.runInWriteLock(() -> super.setLogging(logging), "setLogging(LoggingInfo)");
    }

    @Override
    public void save(LoggingInfo logging) {
        lockingSupport.runInWriteLock(() -> super.save(logging), "save(LoggingInfo)");
    }

    @Override
    public void add(ServiceInfo service) {
        lockingSupport.runInWriteLock(
                () -> super.add(service),
                "add(%s[%s])".formatted(typeOf(service), nameOf(service)));
    }

    @Override
    public void remove(ServiceInfo service) {
        lockingSupport.runInWriteLock(
                () -> super.remove(service),
                "remove(%s[%s])".formatted(typeOf(service), nameOf(service)));
    }

    @Override
    public void save(ServiceInfo service) {
        lockingSupport.runInWriteLock(
                () -> super.save(service),
                "save(%s[%s])".formatted(typeOf(service), nameOf(service)));
    }
}
