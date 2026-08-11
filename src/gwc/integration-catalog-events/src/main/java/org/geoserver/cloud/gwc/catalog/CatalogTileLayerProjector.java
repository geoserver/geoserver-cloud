/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.catalog;

import java.util.List;
import java.util.function.Function;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.cloud.event.catalog.CatalogInfoAdded;
import org.geoserver.cloud.event.catalog.CatalogInfoModified;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.springframework.context.event.EventListener;

/**
 * Projects distributed GeoServer catalog events into the tile-layer state owned by the GWC service.
 *
 * <p>Events are treated as notifications. Add and modify events resolve the latest catalog object by stable identifier
 * and reconcile it with the current GWC configuration. Existing tile-layer settings are never regenerated or
 * overwritten.
 */
@Slf4j(topic = "org.geoserver.cloud.gwc.catalog")
public class CatalogTileLayerProjector {

    private final @NonNull Catalog catalog;
    private final @NonNull GWC gwc;
    private final @NonNull Function<PublishedInfo, GeoServerTileLayer> tileLayerFactory;

    public CatalogTileLayerProjector(@NonNull Catalog catalog, @NonNull GWC gwc) {
        this(catalog, gwc, published -> new GeoServerTileLayer(published, gwc.getConfig(), gwc.getGridSetBroker()));
    }

    CatalogTileLayerProjector(
            @NonNull Catalog catalog,
            @NonNull GWC gwc,
            @NonNull Function<PublishedInfo, GeoServerTileLayer> tileLayerFactory) {
        this.catalog = catalog;
        this.gwc = gwc;
        this.tileLayerFactory = tileLayerFactory;
    }

    @EventListener(CatalogInfoAdded.class)
    public void onCatalogInfoAdded(CatalogInfoAdded event) {
        reconcile(event.getObjectType(), event.getObjectId());
    }

    @EventListener(CatalogInfoModified.class)
    public void onCatalogInfoModified(CatalogInfoModified event) {
        ConfigInfoType type = event.getObjectType();
        PublishedInfo published = resolve(type, event.getObjectId());
        if (published == null) {
            warnNotVisible(type, event.getObjectId());
            return;
        }

        String currentName = tileLayerName(published);
        String oldName = qualifyOldName(event.getOldName(), currentName);
        if (!oldName.equals(currentName) && gwc.tileLayerExists(oldName)) {
            log.debug("Renaming GWC tile layer {} to {}", oldName, currentName);
            gwc.rename(oldName, currentName);
            return;
        }
        reconcile(published, event.getObjectId());
    }

    @EventListener(CatalogInfoRemoved.class)
    public void onCatalogInfoRemoved(CatalogInfoRemoved event) {
        if (!isPublished(event.getObjectType())) {
            return;
        }

        String layerName = event.getObjectName();
        if (gwc.tileLayerExists(layerName)) {
            log.debug("Removing GWC tile layer {} after catalog removal", layerName);
            gwc.removeTileLayers(List.of(layerName));
        }
    }

    private void reconcile(ConfigInfoType type, String publishedId) {
        PublishedInfo published = resolve(type, publishedId);
        if (published == null) {
            warnNotVisible(type, publishedId);
            return;
        }
        reconcile(published, publishedId);
    }

    private void reconcile(PublishedInfo published, String publishedId) {
        GeoServerTileLayer existing = existingTileLayer(published);
        if (existing != null) {
            return;
        }

        GWCConfig config = gwc.getConfig();
        if (!config.isSane() || !config.isCacheLayersByDefault()) {
            return;
        }

        log.debug("Creating automatic GWC tile layer for catalog object {}", publishedId);
        gwc.add(tileLayerFactory.apply(published));
    }

    private GeoServerTileLayer existingTileLayer(PublishedInfo published) {
        if (!gwc.hasTileLayer(published)) {
            return null;
        }
        return gwc.getTileLayer(published);
    }

    private void warnNotVisible(ConfigInfoType type, String publishedId) {
        if (isPublished(type)) {
            log.warn("Unable to project catalog event for {} {}: the catalog object is not visible", type, publishedId);
        }
    }

    private static String qualifyOldName(String oldName, String currentName) {
        int prefixEnd = currentName.indexOf(':');
        if (prefixEnd > 0 && oldName.indexOf(':') < 0) {
            return currentName.substring(0, prefixEnd + 1) + oldName;
        }
        return oldName;
    }

    private PublishedInfo resolve(ConfigInfoType type, String publishedId) {
        return switch (type) {
            case LAYER -> catalog.getLayer(publishedId);
            case LAYERGROUP -> catalog.getLayerGroup(publishedId);
            default -> null;
        };
    }

    private static boolean isPublished(ConfigInfoType type) {
        return type == ConfigInfoType.LAYER || type == ConfigInfoType.LAYERGROUP;
    }

    private static String tileLayerName(PublishedInfo published) {
        return switch (published) {
            case LayerInfo layer -> GWC.tileLayerName(layer);
            case LayerGroupInfo group -> GWC.tileLayerName(group);
            default -> throw new IllegalArgumentException("Unsupported published object: " + published);
        };
    }
}
