/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.repository;

import static org.geoserver.cloud.gwc.event.GeoWebCacheEvent.Type.CREATED;
import static org.geoserver.cloud.gwc.event.GeoWebCacheEvent.Type.DELETED;
import static org.geoserver.cloud.gwc.event.GeoWebCacheEvent.Type.MODIFIED;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.geoserver.cloud.gwc.event.BlobStoreEvent;
import org.geoserver.cloud.gwc.event.GeoWebCacheEvent;
import org.geoserver.cloud.gwc.event.GridsetEvent;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.config.BlobStoreConfigurationListener;
import org.geowebcache.config.BlobStoreInfo;
import org.geowebcache.config.ConfigurationResourceProvider;
import org.geowebcache.config.ListenerCollection;
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

    private ListenerCollection<BlobStoreConfigurationListener> spiedListeners;

    public CloudGwcXmlConfiguration( //
            ApplicationContextProvider appCtx, ConfigurationResourceProvider inFac) {
        this(appCtx, inFac, appCtx.getApplicationContext()::publishEvent);
    }

    @SuppressWarnings("unchecked")
    public CloudGwcXmlConfiguration( //
            ApplicationContextProvider appCtx, //
            ConfigurationResourceProvider inFac, //
            @NonNull Consumer<GeoWebCacheEvent> publisher) {

        super(appCtx, inFac);
        this.publisher = publisher;

        try {
            spiedListeners = (ListenerCollection<BlobStoreConfigurationListener>)
                    FieldUtils.readField(this, "blobStoreListeners", true);

        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @EventListener(GridsetEvent.class)
    public boolean onGridsetEvent(GridsetEvent event) throws Exception {
        final boolean isRemote = !isLocal(event);
        if (isRemote) {
            reload(event);
        }
        return isRemote;
    }

    @EventListener(BlobStoreEvent.class)
    public boolean onBlobStoreEvent(BlobStoreEvent event) throws Exception {
        if (isLocal(event)) {
            return false;
        }

        final String blobStoreId = event.getBlobStoreId();
        final Optional<BlobStoreInfo> pre = super.getBlobStore(blobStoreId);

        reload(event);

        final Optional<BlobStoreInfo> post = super.getBlobStore(blobStoreId);

        // relay events to BlobStoreConfigurationListener, especially to CompositeBlobStore, which
        // holds an internal Map of BlobStores
        switch (event.getEventType()) {
            case DELETED:
                spiedListeners.safeForEach(l -> l.handleRemoveBlobStore(pre.orElseThrow()));
                break;
            case CREATED:
                spiedListeners.safeForEach(l -> l.handleAddBlobStore(post.orElseThrow()));
                break;
            case MODIFIED:
                String oldName = event.getOldName();
                if (null == oldName) {
                    spiedListeners.safeForEach(l -> l.handleModifyBlobStore(post.orElseThrow()));
                } else {
                    spiedListeners.safeForEach(l -> l.handleRenameBlobStore(oldName, post.orElseThrow()));
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
        }
        return true;
    }

    private boolean isLocal(GeoWebCacheEvent event) {
        return event.getSource() == this;
    }

    private void reload(GeoWebCacheEvent event) throws Exception {
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

    @Override
    public void addGridSet(GridSet gridSet) {
        lock.writeLock().lock();
        try {
            super.addGridSet(gridSet);
        } finally {
            lock.writeLock().unlock();
        }
        publisher.accept(new GridsetEvent(this, CREATED, gridSet.getName()));
    }

    @Override
    public void modifyGridSet(GridSet gridSet) {
        lock.writeLock().lock();
        try {
            super.modifyGridSet(gridSet);
        } finally {
            lock.writeLock().unlock();
        }
        publisher.accept(new GridsetEvent(this, MODIFIED, gridSet.getName()));
    }

    @Override
    public void removeGridSet(String gridSetName) {
        lock.writeLock().lock();
        try {
            super.removeGridSet(gridSetName);
        } finally {
            lock.writeLock().unlock();
        }
        publisher.accept(new GridsetEvent(this, DELETED, gridSetName));
    }

    @Override
    public void addBlobStore(final BlobStoreInfo bs) throws IllegalArgumentException {
        lock.writeLock().lock();
        try {
            super.addBlobStore(bs);
        } finally {
            lock.writeLock().unlock();
        }
        publisher.accept(new BlobStoreEvent(this, CREATED, bs.getName()));
    }

    @Override
    public void modifyBlobStore(final BlobStoreInfo bs) {
        lock.writeLock().lock();
        try {
            super.modifyBlobStore(bs);
        } finally {
            lock.writeLock().unlock();
        }
        publisher.accept(new BlobStoreEvent(this, MODIFIED, bs.getName()));
    }

    @Override
    public void renameBlobStore(final String oldName, final String newName) {
        lock.writeLock().lock();
        try {
            super.renameBlobStore(oldName, newName);
        } finally {
            lock.writeLock().unlock();
        }
        publisher.accept(new BlobStoreEvent(this, MODIFIED, oldName, newName));
    }

    @Override
    public void removeBlobStore(final String blobStoreName) {
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

    @Override
    public void afterPropertiesSet() throws GeoWebCacheException {
        lock.writeLock().lock();
        try {
            super.afterPropertiesSet();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Boolean isRuntimeStatsEnabled() {
        return runInReadLock(super::isRuntimeStatsEnabled);
    }

    @Override
    public void setRuntimeStatsEnabled(Boolean isEnabled) throws IOException {
        lock.writeLock().lock();
        try {
            super.setRuntimeStatsEnabled(isEnabled);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public ServiceInformation getServiceInformation() {
        return runInReadLock(super::getServiceInformation);
    }

    @Override
    public void setServiceInformation(ServiceInformation serviceInfo) throws IOException {
        lock.writeLock().lock();
        try {
            super.setServiceInformation(serviceInfo);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void setDefaultValues(TileLayer layer) {
        lock.writeLock().lock();
        try {
            super.setDefaultValues(layer);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void addLayer(TileLayer tl) throws IllegalArgumentException {
        lock.writeLock().lock();
        try {
            super.addLayer(tl);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void modifyLayer(TileLayer tl) throws NoSuchElementException {
        lock.writeLock().lock();
        try {
            super.modifyLayer(tl);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void renameLayer(String oldName, String newName) throws NoSuchElementException, IllegalArgumentException {
        lock.writeLock().lock();
        try {
            super.renameLayer(oldName, newName);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void removeLayer(final String layerName) throws NoSuchElementException, IllegalArgumentException {
        lock.writeLock().lock();
        try {
            super.removeLayer(layerName);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Collection<TileLayer> getLayers() {
        return runInReadLock(super::getLayers);
    }

    @Override
    public Optional<TileLayer> getLayer(String layerName) {
        return runInReadLock(() -> super.getLayer(layerName));
    }

    @SuppressWarnings("deprecation")
    @Override
    public TileLayer getTileLayer(String layerName) {
        return runInReadLock(() -> super.getTileLayer(layerName));
    }

    @SuppressWarnings("deprecation")
    @Override
    public TileLayer getTileLayerById(String layerId) {
        return runInReadLock(() -> super.getTileLayerById(layerId));
    }

    @Override
    public boolean containsLayer(String layerId) {
        return runInReadLock(() -> super.containsLayer(layerId));
    }

    @Override
    public int getLayerCount() {
        return runInReadLock(super::getLayerCount);
    }

    @Override
    public Set<String> getLayerNames() {
        return runInReadLock(super::getLayerNames);
    }

    @Override
    public String getVersion() {
        return runInReadLock(super::getVersion);
    }

    @Override
    public Boolean isFullWMS() {
        return runInReadLock(super::isFullWMS);
    }

    @Override
    public void setFullWMS(Boolean isFullWMS) throws IOException {
        lock.writeLock().lock();
        try {
            super.setFullWMS(isFullWMS);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<BlobStoreInfo> getBlobStores() {
        return runInReadLock(super::getBlobStores);
    }

    @Override
    public int getBlobStoreCount() {
        return runInReadLock(super::getBlobStoreCount);
    }

    @Override
    public Set<String> getBlobStoreNames() {
        return runInReadLock(super::getBlobStoreNames);
    }

    @Override
    public Optional<BlobStoreInfo> getBlobStore(String name) {
        return runInReadLock(() -> super.getBlobStore(name));
    }

    @Override
    public boolean containsBlobStore(String name) {
        return runInReadLock(() -> super.containsBlobStore(name));
    }

    @Override
    public LockProvider getLockProvider() {
        return runInReadLock(super::getLockProvider);
    }

    @Override
    public void setLockProvider(LockProvider lockProvider) throws IOException {
        lock.writeLock().lock();
        try {
            super.setLockProvider(lockProvider);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Boolean isWmtsCiteCompliant() {
        return runInReadLock(super::isWmtsCiteCompliant);
    }

    @Override
    public void setWmtsCiteCompliant(Boolean wmtsCiteStrictCompliant) throws IOException {
        lock.writeLock().lock();
        try {
            super.setWmtsCiteCompliant(wmtsCiteStrictCompliant);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Integer getBackendTimeout() {
        return runInReadLock(super::getBackendTimeout);
    }

    @Override
    public void setBackendTimeout(Integer backendTimeout) throws IOException {
        lock.writeLock().lock();
        try {
            super.setBackendTimeout(backendTimeout);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Boolean isCacheBypassAllowed() {
        return runInReadLock(super::isCacheBypassAllowed);
    }

    @Override
    public void setCacheBypassAllowed(Boolean cacheBypassAllowed) throws IOException {
        lock.writeLock().lock();
        try {
            super.setCacheBypassAllowed(cacheBypassAllowed);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<GridSet> getGridSet(String name) {
        return runInReadLock(() -> super.getGridSet(name));
    }

    @Override
    public Collection<GridSet> getGridSets() {
        return runInReadLock(super::getGridSets);
    }
}
