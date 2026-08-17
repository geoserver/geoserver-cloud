/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.repository;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.cache.LoadingCache;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.cloud.gwc.event.TileLayerEvent;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.gwc.layer.TileLayerInfoUtil;
import org.geoserver.util.DimensionWarning.WarningType;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.ExpirationRule;
import org.geowebcache.layer.TileLayer;
import org.springframework.context.event.EventListener;

/**
 * @since 1.0
 */
@Slf4j(topic = "org.geoserver.cloud.gwc.repository")
public class CloudCatalogConfiguration extends CatalogConfiguration {

    private LoadingCache<String, GeoServerTileLayer> spiedLayerCache;

    private final TileLayerCatalog tileLayerCatalog;

    @SuppressWarnings("unchecked")
    public CloudCatalogConfiguration(Catalog catalog, TileLayerCatalog tileLayerCatalog, GridSetBroker gridSetBroker) {

        super(catalog, tileLayerCatalog, gridSetBroker);
        this.tileLayerCatalog = tileLayerCatalog;

        try {
            spiedLayerCache = (LoadingCache<String, GeoServerTileLayer>) FieldUtils.readField(this, "layerCache", true);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Listen to {@link TileLayerEvent}s and clear the cached {@link TileLayer}
     *
     * <p>Important: this will only work of this object is a spring bean, if it's not (e.g. used as
     * delegate for a decorator), make sure it calls this method as appropriate.
     */
    @EventListener(TileLayerEvent.class)
    public void onTileLayerEventEvict(TileLayerEvent event) {
        String publishedId = event.getPublishedId();
        log.debug("evicting GeoServerTileLayer[{}] cache entry upon {}", publishedId, event);
        spiedLayerCache.invalidate(publishedId);
        spiedLayerCache.getIfPresent(publishedId);
    }

    /**
     * In a cluster, a tile layer configuration can be visible through the shared storage or a {@link TileLayerEvent}
     * before this node's catalog replicated the {@link PublishedInfo} it refers to. Upstream assumes both are updated
     * atomically and {@link GeoServerTileLayer#getPublishedInfo()} throws {@link IllegalStateException}, which would
     * break whole responses like WMTS GetCapabilities. Hide such tile layers until the local catalog catches up.
     */
    @Override
    public Optional<TileLayer> getLayer(final String layerName) {
        return super.getLayer(layerName).filter(this::publishedInfoResolves);
    }

    /** Keeps the name listing consistent with {@link #getLayer}, excluding hidden tile layers. */
    @Override
    public Set<String> getLayerNames() {
        return super.getLayerNames().stream()
                .filter(name -> getLayer(name).isPresent())
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Keeps the count consistent with {@link #getLayerNames}, excluding hidden tile layers. */
    @Override
    public int getLayerCount() {
        return getLayerNames().size();
    }

    /** Keeps the containment check consistent with {@link #getLayer}, excluding hidden tile layers. */
    @Override
    public boolean containsLayer(String layerName) {
        return getLayer(layerName).isPresent();
    }

    private boolean publishedInfoResolves(TileLayer tileLayer) {
        if (tileLayer instanceof GeoServerTileLayer geoserverTileLayer) {
            try {
                return null != geoserverTileLayer.getPublishedInfo();
            } catch (IllegalStateException catalogNotYetInSync) {
                log.debug(
                        "tile layer {} does not resolve against the local catalog yet, hiding it until the catalog catches up ({})",
                        tileLayer.getName(),
                        catalogNotYetInSync.getMessage());
                return false;
            }
        }
        return true;
    }

    @Override
    public void addLayer(final @NonNull TileLayer tl) {
        GeoServerTileLayer tileLayer = checkAddPreconditions(tl);
        // In a cluster the configuration may already be stored when an add arrives: automatic tile layer
        // creation on another service, or a client told the layer does not exist while this node was
        // catching up (catalog replication, a configuration reload in progress). Honor the add as a save,
        // inheriting whatever the request leaves unset from the stored configuration, instead of failing
        // the upstream already-exists precondition.
        GeoServerTileLayerInfo stored =
                tileLayerCatalog.getLayerById(tileLayer.getInfo().getId());
        if (stored != null) {
            setMissingConfig(tileLayer.getInfo(), stored);
            super.modifyLayer(tl);
            return;
        }
        completeWithDefaults(tileLayer);
        super.addLayer(tl);
    }

    private GeoServerTileLayer checkAddPreconditions(@NonNull TileLayer tl) {
        checkArgument(canSave(tl), "Can't save TileLayer of type ", tl.getClass());

        GeoServerTileLayer tileLayer = (GeoServerTileLayer) tl;
        GeoServerTileLayerInfo info = tileLayer.getInfo();

        checkNotNull(info, "GeoServerTileLayerInfo is null");
        checkNotNull(info.getId(), "id is null");
        checkNotNull(info.getName(), "name is null");
        return tileLayer;
    }

    private void completeWithDefaults(GeoServerTileLayer tileLayer) {
        GeoServerTileLayerInfo info = tileLayer.getInfo();
        PublishedInfo publishedInfo = tileLayer.getPublishedInfo();
        GWCConfig defaults = GWC.get().getConfig();
        GeoServerTileLayerInfo infoDefaults = TileLayerInfoUtil.loadOrCreate(publishedInfo, defaults);
        setMissingConfig(info, infoDefaults);
    }

    public static void setMissingConfig(GeoServerTileLayerInfo info, GeoServerTileLayerInfo defaults) {

        String blobStoreId = defaults.getBlobStoreId();
        Set<WarningType> cacheWarningSkips = defaults.getCacheWarningSkips();
        int expireCache = defaults.getExpireCache();
        List<ExpirationRule> expireCacheList = defaults.getExpireCacheList();
        int expireClients = defaults.getExpireClients();
        Set<XMLGridSubset> gridSubsets = defaults.getGridSubsets();
        int gutter = defaults.getGutter();
        int metaTilingX = defaults.getMetaTilingX();
        int metaTilingY = defaults.getMetaTilingY();
        Set<String> mimeFormats = defaults.getMimeFormats();
        Set<ParameterFilter> parameterFilters = defaults.getParameterFilters();

        if (null == info.getBlobStoreId()) {
            info.setBlobStoreId(blobStoreId);
        }
        if (null == info.getCacheWarningSkips()) {
            info.setCacheWarningSkips(cacheWarningSkips);
        }
        if (0 == info.getExpireCache()) {
            info.setExpireCache(expireCache);
        }
        if (null == info.getExpireCacheList()) {
            info.setExpireCacheList(expireCacheList);
        }
        if (0 == info.getExpireClients()) {
            info.setExpireClients(expireClients);
        }
        if (null == info.getGridSubsets() || info.getGridSubsets().isEmpty()) {
            info.setGridSubsets(gridSubsets);
        }
        if (0 == info.getGutter()) {
            info.setGutter(gutter);
        }
        if (0 == info.getMetaTilingX()) {
            info.setMetaTilingX(metaTilingX);
        }
        if (0 == info.getMetaTilingY()) {
            info.setMetaTilingY(metaTilingY);
        }
        if (info.getMimeFormats().isEmpty()) {
            info.getMimeFormats().addAll(mimeFormats);
        }
        if (null == info.getParameterFilters() || info.getParameterFilters().isEmpty()) {
            info.setParameterFilters(parameterFilters);
        }
    }
}
