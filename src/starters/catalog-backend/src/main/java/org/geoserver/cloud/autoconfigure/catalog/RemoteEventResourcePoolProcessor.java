/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.cloud.event.catalog.CatalogInfoAddEvent;
import org.geoserver.cloud.event.catalog.CatalogInfoModifyEvent;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoveEvent;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import java.lang.reflect.Proxy;

/**
 * Cleans up cached {@link ResourcePool} entries upon remote {@link CatalogInfoAddEvent}s, {@link
 * CatalogInfoModifyEvent}s, and {@link CatalogInfoRemoveEvent}s.
 *
 * @since 1.0
 */
@Slf4j(topic = "org.geoserver.cloud.bus.incoming")
public class RemoteEventResourcePoolProcessor {

    private Catalog rawCatalog;

    /**
     * @param rawCatalog used to evict cached live data sources from its {@link
     *     Catalog#getResourcePool() ResourcePool}
     */
    public @Autowired RemoteEventResourcePoolProcessor(Catalog rawCatalog) {
        this.rawCatalog = rawCatalog;
    }

    /**
     * no-op, really, what do we care if a CatalogInfo has been added until an incoming service
     * request needs it
     */
    @EventListener(CatalogInfoAddEvent.class)
    public void onCatalogRemoteAddEvent(CatalogInfoAddEvent event) {
        evictFromResourcePool(event);
    }

    @EventListener(CatalogInfoRemoveEvent.class)
    public void onCatalogRemoteRemoveEvent(CatalogInfoRemoveEvent event) {
        evictFromResourcePool(event);
    }

    @EventListener(CatalogInfoModifyEvent.class)
    public void onCatalogRemoteModifyEvent(CatalogInfoModifyEvent event) {
        evictFromResourcePool(event);
    }

    private void evictFromResourcePool(InfoEvent<?, Catalog, CatalogInfo> event) {
        event.remote()
                .ifPresentOrElse(
                        remoteEvent -> {
                            final String id = event.getObjectId();
                            final ConfigInfoType infoType = event.getObjectType();
                            switch (infoType) {
                                case CoverageStoreInfo:
                                case DataStoreInfo:
                                case WmsStoreInfo:
                                case WmtsStoreInfo:
                                case FeatureTypeInfo:
                                case StyleInfo:
                                    log.debug("Evict ResourcePool cache for {}", event);
                                    doEvict(id, infoType);
                                    break;
                                default:
                                    log.trace(
                                            "no need to clear resource pool cache entry for object of type {}",
                                            infoType);
                                    break;
                            }
                        },
                        () -> log.trace("Ignoring event from self: {}", event));
    }

    private void doEvict(String id, ConfigInfoType catalogInfoEnumType) {
        ResourcePool resourcePool = rawCatalog.getResourcePool();
        CatalogInfo catalogInfo = proxyInstanceOf(id, catalogInfoEnumType);
        switch (catalogInfoEnumType) {
            case CoverageStoreInfo:
                resourcePool.clear((CoverageStoreInfo) catalogInfo);
                break;
            case DataStoreInfo:
                resourcePool.clear((DataStoreInfo) catalogInfo);
                break;
            case FeatureTypeInfo:
                resourcePool.clear((FeatureTypeInfo) catalogInfo);
                break;
            case StyleInfo:
                // HACK: resourcePool.clear(StyleInfo) is key'ed by the object itself not the id
                StyleInfo style = rawCatalog.getStyle(catalogInfo.getId());
                if (style != null) {
                    resourcePool.clear(style);
                }
                break;
            case WmsStoreInfo:
                resourcePool.clear((WMSStoreInfo) catalogInfo);
                break;
            case WmtsStoreInfo:
                resourcePool.clear((WMTSStoreInfo) catalogInfo);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private CatalogInfo proxyInstanceOf(final String id, final ConfigInfoType catalogInfoType) {

        Class<? extends Info> infoInterface = catalogInfoType.getType();

        return (CatalogInfo)
                Proxy.newProxyInstance(
                        getClass().getClassLoader(),
                        new Class[] {infoInterface},
                        (proxy, method, args) -> {
                            if (method.getName().equals("getId")) {
                                return id;
                            }
                            return null;
                        });
    }
}
