/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.locking;

import static org.geoserver.cloud.catalog.locking.LockingSupport.nameOf;
import static org.geoserver.cloud.catalog.locking.LockingSupport.typeOf;

import static java.lang.String.format;

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

    public @Override void setGlobal(GeoServerInfo global) {
        lockingSupport.runInWriteLock(
                () -> super.setGlobal(global), format("setGlobal(%s)", nameOf(global)));
    }

    public @Override void save(GeoServerInfo geoServer) {
        lockingSupport.runInWriteLock(
                () -> super.save(geoServer), format("save(%s)", nameOf(geoServer)));
    }

    public @Override void add(SettingsInfo settings) {
        lockingSupport.runInWriteLock(
                () -> super.add(settings),
                format("add(%s[%s])", typeOf(settings), nameOf(settings)));
    }

    public @Override void save(SettingsInfo settings) {
        lockingSupport.runInWriteLock(
                () -> super.save(settings),
                format("save(%s[%s])", typeOf(settings), nameOf(settings)));
    }

    public @Override void remove(SettingsInfo settings) {
        lockingSupport.runInWriteLock(
                () -> super.remove(settings),
                format("remove(%s[%s])", typeOf(settings), nameOf(settings)));
    }

    public @Override void setLogging(LoggingInfo logging) {
        lockingSupport.runInWriteLock(() -> super.setLogging(logging), "setLogging(LoggingInfo)");
    }

    public @Override void save(LoggingInfo logging) {
        lockingSupport.runInWriteLock(() -> super.setLogging(logging), "save(LoggingInfo)");
    }

    public @Override void add(ServiceInfo service) {
        lockingSupport.runInWriteLock(
                () -> super.add(service), format("add(%s[%s])", typeOf(service), nameOf(service)));
    }

    public @Override void remove(ServiceInfo service) {
        lockingSupport.runInWriteLock(
                () -> super.remove(service),
                format("remove(%s[%s])", typeOf(service), nameOf(service)));
    }

    public @Override void save(ServiceInfo service) {
        lockingSupport.runInWriteLock(
                () -> super.save(service),
                format("save(%s[%s])", typeOf(service), nameOf(service)));
    }
}
