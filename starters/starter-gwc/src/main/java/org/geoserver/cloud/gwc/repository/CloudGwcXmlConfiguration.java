/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.repository;

import static org.geoserver.cloud.gwc.event.GeoWebCacheEvent.Type.CREATED;
import static org.geoserver.cloud.gwc.event.GeoWebCacheEvent.Type.DELETED;
import static org.geoserver.cloud.gwc.event.GeoWebCacheEvent.Type.MODIFIED;

import com.google.common.base.Supplier;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.gwc.event.BlobStoreEvent;
import org.geoserver.cloud.gwc.event.GeoWebCacheEvent;
import org.geoserver.cloud.gwc.event.GridsetEvent;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.config.BlobStoreConfigurationListener;
import org.geowebcache.config.BlobStoreInfo;
import org.geowebcache.config.ConfigurationResourceProvider;
import org.geowebcache.config.XMLConfiguration;
import org.geowebcache.config.meta.ServiceInformation;
import org.geowebcache.grid.GridSet;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.locks.LockProvider;
import org.geowebcache.util.ApplicationContextProvider;
import org.springframework.context.event.EventListener;

/**
 * @implNote there is a {@link BlobStoreConfigurationListener} abstraction, but no homologous one to
 *     listen to gridset configuration changes, so for the sake of consistency, we're overriding the
 *     blobstore and gridset add/remove/modify methods as decorator throwing {@link GridsetEvent}
 *     and {@link BlobStoreEvent} appropriately.
 * @since 1.0
 */
@Slf4j(topic = "org.geoserver.cloud.gwc.repository")
public class CloudGwcXmlConfiguration extends XMLConfiguration {

    private final @NonNull Consumer<GeoWebCacheEvent> publisher;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public CloudGwcXmlConfiguration( //
            ApplicationContextProvider appCtx, ConfigurationResourceProvider inFac) {
        this(appCtx, inFac, appCtx.getApplicationContext()::publishEvent);
    }

    public CloudGwcXmlConfiguration( //
            ApplicationContextProvider appCtx, //
            ConfigurationResourceProvider inFac, //
            @NonNull Consumer<GeoWebCacheEvent> publisher) {

        super(appCtx, inFac);
        this.publisher = publisher;
    }

    @EventListener(GridsetEvent.class)
    public boolean onGridsetEvent(GridsetEvent event) throws Exception {
        if (event.getSource() == this) return false;
        switch (event.getEventType()) {
            case CREATED:
            case DELETED:
            case MODIFIED:
                reload(event);
                return true;
            default:
                throw new IllegalArgumentException("Uknown event type: " + event.getEventType());
        }
    }

    @EventListener(BlobStoreEvent.class)
    public boolean onBlobStoreEvent(BlobStoreEvent event) throws Exception {
        if (event.getSource() == this) return false;
        switch (event.getEventType()) {
            case CREATED:
            case DELETED:
            case MODIFIED:
                reload(event);
                return true;
            default:
                throw new IllegalArgumentException("Uknown event type: " + event.getEventType());
        }
    }

    private synchronized void reload(GeoWebCacheEvent event) throws Exception {
        log.info("reloading {} configuration upon {}", getConfigLocation(), event);
        lock.writeLock().lock();
        try {
            super.deinitialize();
            super.reinitialize();
        } finally {
            lock.writeLock().unlock();
        }
        log.debug("configuration reloaded successfully");
    }

    public @Override void addGridSet(GridSet gridSet) {
        lock.writeLock().lock();
        try {
            super.addGridSet(gridSet);
        } finally {
            lock.writeLock().unlock();
        }
        publisher.accept(new GridsetEvent(this, CREATED, gridSet.getName()));
    }

    public @Override void modifyGridSet(GridSet gridSet) {
        lock.writeLock().lock();
        try {
            super.modifyGridSet(gridSet);
        } finally {
            lock.writeLock().unlock();
        }
        publisher.accept(new GridsetEvent(this, MODIFIED, gridSet.getName()));
    }

    public @Override void removeGridSet(String gridSetName) {
        lock.writeLock().lock();
        try {
            super.removeGridSet(gridSetName);
        } finally {
            lock.writeLock().unlock();
        }
        publisher.accept(new GridsetEvent(this, DELETED, gridSetName));
    }

    public @Override void addBlobStore(final BlobStoreInfo bs) throws IllegalArgumentException {
        lock.writeLock().lock();
        try {
            super.addBlobStore(bs);
        } finally {
            lock.writeLock().unlock();
        }
        publisher.accept(new BlobStoreEvent(this, CREATED, bs.getName()));
    }

    public @Override void modifyBlobStore(final BlobStoreInfo bs) {
        lock.writeLock().lock();
        try {
            super.modifyBlobStore(bs);
        } finally {
            lock.writeLock().unlock();
        }
        publisher.accept(new BlobStoreEvent(this, MODIFIED, bs.getName()));
    }

    public @Override void renameBlobStore(final String oldName, final String newName) {
        lock.writeLock().lock();
        try {
            super.renameBlobStore(oldName, newName);
        } finally {
            lock.writeLock().unlock();
        }
        publisher.accept(new BlobStoreEvent(this, MODIFIED, newName));
    }

    public @Override void removeBlobStore(final String blobStoreName) {
        lock.writeLock().lock();
        try {
            super.removeBlobStore(blobStoreName);
        } finally {
            lock.writeLock().unlock();
        }
        publisher.accept(new BlobStoreEvent(this, DELETED, blobStoreName));
    }

    /*
     * All the following public methods are overrides to acquire a read or write lock as
     * appropriate, because some way or another they interact with an instance variable
     * (gridSets,layers, gwcConfig), that can be temporarily null while the config is
     * deinitialize()'d and reloaded due to an external configuration event
     */

    private <T> T runInReadLock(Supplier<T> action) {
        lock.readLock().lock();
        try {
            return action.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    public @Override void afterPropertiesSet() throws GeoWebCacheException {
        lock.writeLock().lock();
        try {
            super.afterPropertiesSet();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public @Override Boolean isRuntimeStatsEnabled() {
        return runInReadLock(super::isRuntimeStatsEnabled);
    }

    public @Override void setRuntimeStatsEnabled(Boolean isEnabled) throws IOException {
        lock.writeLock().lock();
        try {
            super.setRuntimeStatsEnabled(isEnabled);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public @Override ServiceInformation getServiceInformation() {
        return runInReadLock(super::getServiceInformation);
    }

    public @Override void setServiceInformation(ServiceInformation serviceInfo) throws IOException {
        lock.writeLock().lock();
        try {
            super.setServiceInformation(serviceInfo);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public @Override void setDefaultValues(TileLayer layer) {
        lock.writeLock().lock();
        try {
            super.setDefaultValues(layer);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public @Override void addLayer(TileLayer tl) throws IllegalArgumentException {
        lock.writeLock().lock();
        try {
            super.addLayer(tl);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public @Override void modifyLayer(TileLayer tl) throws NoSuchElementException {
        lock.writeLock().lock();
        try {
            super.modifyLayer(tl);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public @Override void renameLayer(String oldName, String newName)
            throws NoSuchElementException, IllegalArgumentException {
        lock.writeLock().lock();
        try {
            super.renameLayer(oldName, newName);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public @Override void removeLayer(final String layerName)
            throws NoSuchElementException, IllegalArgumentException {
        lock.writeLock().lock();
        try {
            super.removeLayer(layerName);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public @Override Collection<TileLayer> getLayers() {
        return runInReadLock(super::getLayers);
    }

    public @Override Optional<TileLayer> getLayer(String layerName) {
        return runInReadLock(() -> super.getLayer(layerName));
    }

    @SuppressWarnings("deprecation")
    public @Override TileLayer getTileLayer(String layerName) {
        return runInReadLock(() -> super.getTileLayer(layerName));
    }

    @SuppressWarnings("deprecation")
    public @Override TileLayer getTileLayerById(String layerId) {
        return runInReadLock(() -> super.getTileLayerById(layerId));
    }

    public @Override boolean containsLayer(String layerId) {
        return runInReadLock(() -> super.containsLayer(layerId));
    }

    public @Override int getLayerCount() {
        return runInReadLock(super::getLayerCount);
    }

    public @Override Set<String> getLayerNames() {
        return runInReadLock(super::getLayerNames);
    }

    public @Override String getVersion() {
        return runInReadLock(super::getVersion);
    }

    public @Override Boolean isFullWMS() {
        return runInReadLock(super::isFullWMS);
    }

    public @Override void setFullWMS(Boolean isFullWMS) throws IOException {
        lock.writeLock().lock();
        try {
            super.setFullWMS(isFullWMS);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public @Override List<BlobStoreInfo> getBlobStores() {
        return runInReadLock(super::getBlobStores);
    }

    public @Override int getBlobStoreCount() {
        return runInReadLock(super::getBlobStoreCount);
    }

    public @Override Set<String> getBlobStoreNames() {
        return runInReadLock(super::getBlobStoreNames);
    }

    public @Override Optional<BlobStoreInfo> getBlobStore(String name) {
        return runInReadLock(() -> super.getBlobStore(name));
    }

    public @Override boolean containsBlobStore(String name) {
        return runInReadLock(() -> super.containsBlobStore(name));
    }

    public @Override LockProvider getLockProvider() {
        return runInReadLock(super::getLockProvider);
    }

    public @Override void setLockProvider(LockProvider lockProvider) throws IOException {
        lock.writeLock().lock();
        try {
            super.setLockProvider(lockProvider);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public @Override Boolean isWmtsCiteCompliant() {
        return runInReadLock(super::isWmtsCiteCompliant);
    }

    public @Override void setWmtsCiteCompliant(Boolean wmtsCiteStrictCompliant) throws IOException {
        lock.writeLock().lock();
        try {
            super.setWmtsCiteCompliant(wmtsCiteStrictCompliant);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public @Override Integer getBackendTimeout() {
        return runInReadLock(super::getBackendTimeout);
    }

    public @Override void setBackendTimeout(Integer backendTimeout) throws IOException {
        lock.writeLock().lock();
        try {
            super.setBackendTimeout(backendTimeout);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public @Override Boolean isCacheBypassAllowed() {
        return runInReadLock(super::isCacheBypassAllowed);
    }

    public @Override void setCacheBypassAllowed(Boolean cacheBypassAllowed) throws IOException {
        lock.writeLock().lock();
        try {
            super.setCacheBypassAllowed(cacheBypassAllowed);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public @Override Optional<GridSet> getGridSet(String name) {
        return runInReadLock(() -> super.getGridSet(name));
    }

    public @Override Collection<GridSet> getGridSets() {
        return runInReadLock(super::getGridSets);
    }
}
