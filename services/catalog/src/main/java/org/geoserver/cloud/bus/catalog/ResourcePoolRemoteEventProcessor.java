/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.catalog;

import java.lang.reflect.Proxy;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.bus.BusAutoConfiguration;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.cloud.bus.event.AckRemoteApplicationEvent;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.context.event.EventListener;

/**
 * Listens to {@link CatalogRemoteEvent}s and acts accordingly
 *
 * @implNote {@code spring-cloud-bus} builds an abstraction layer over {@code spring-cloud-stream}
 *     to set up the event bus for the microservices on top of the configured broker (AMQP/Kafka),
 *     handling. See {@link BusAutoConfiguration}, it sets up the bus configuration and makes sure
 *     {@link RemoteApplicationEvent}s are not published to the same instance that broadcast them.
 */
@Slf4j(topic = "org.geoserver.cloud.catalog.bus.incoming")
public class ResourcePoolRemoteEventProcessor {

    private Catalog rawCatalog;

    private @Autowired ServiceMatcher busServiceMatcher;

    public @Autowired ResourcePoolRemoteEventProcessor(Catalog rawCatalog) {
        this.rawCatalog = rawCatalog;
    }

    /**
     * Logs ack events received from nodes which processed events sent by the remote event
     * broadcaster
     *
     * <p>{@code spring.cloud.bus.ack.enabled=true} must be set in order for these events to be
     * processed (see {@link BusAutoConfiguration})
     */
    public @EventListener(AckRemoteApplicationEvent.class) void ackReceived(
            AckRemoteApplicationEvent event) {
        if (!busServiceMatcher.isFromSelf(event)) {
            log.trace("Received event ack {}", event); // TODO improve log statement
        }
    }

    /**
     * no-op, really, what do we care if a CatalogInfo has been added until anincoming service
     * request needs it
     */
    @EventListener(CatalogRemoteAddEvent.class)
    public void onCatalogRemoteAddEvent(CatalogRemoteAddEvent event) {
        if (busServiceMatcher.isFromSelf(event)) {
            log.trace("Ignoring remote event from self: {}", event);
        } else {
            log.debug("remote add event, nothing to do. {}", event);
        }
    }

    @EventListener(CatalogRemoteRemoveEvent.class)
    public void onCatalogRemoteRemoveEvent(CatalogRemoteRemoveEvent event) {
        evictFromResourcePool(event);
    }

    @EventListener(CatalogRemoteModifyEvent.class)
    public void onCatalogRemoteModifyEvent(CatalogRemoteModifyEvent event) {
        evictFromResourcePool(event);
    }

    private void evictFromResourcePool(CatalogRemoteEvent event) {
        if (busServiceMatcher.isFromSelf(event)) {
            log.trace("Ignoring event from self: {}", event);
            return;
        }
        final String id = event.getCatalogInfoId();
        final ClassMappings catalogInfoEnumType = event.getCatalogInfoEnumType();
        switch (catalogInfoEnumType) {
            case COVERAGESTORE:
            case DATASTORE:
            case FEATURETYPE:
            case STYLE:
            case WMSSTORE:
            case WMTSSTORE:
                log.debug("Evict ResourcePool cache for {}", event);
                doEvict(id, catalogInfoEnumType);
                break;
            default:
                log.trace(
                        "no need to clear resource pool cache entry for object of type {}",
                        catalogInfoEnumType);
                break;
        }
    }

    private void doEvict(String id, ClassMappings catalogInfoEnumType) {
        final Class<? extends CatalogInfo> catalogInfoType = catalogInfoEnumType.getInterface();
        ResourcePool resourcePool = rawCatalog.getResourcePool();
        CatalogInfo catalogInfo = proxyInstanceOf(id, catalogInfoType);
        switch (catalogInfoEnumType) {
            case COVERAGESTORE:
                resourcePool.clear((CoverageStoreInfo) catalogInfo);
                break;
            case DATASTORE:
                resourcePool.clear((DataStoreInfo) catalogInfo);
                break;
            case FEATURETYPE:
                resourcePool.clear((FeatureTypeInfo) catalogInfo);
                break;
            case STYLE:
                // HACK: resourcePool.clear(StyleInfo) is key'ed by the object not the id
                StyleInfo style = rawCatalog.getStyle(catalogInfo.getId());
                if (style != null) {
                    resourcePool.clear(style);
                }
                break;
            case WMSSTORE:
                resourcePool.clear((WMSStoreInfo) catalogInfo);
                break;
            case WMTSSTORE:
                resourcePool.clear((WMTSStoreInfo) catalogInfo);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private CatalogInfo proxyInstanceOf(
            final String id, final Class<? extends CatalogInfo> catalogInfoType) {
        return (CatalogInfo)
                Proxy.newProxyInstance(
                        getClass().getClassLoader(),
                        new Class[] {catalogInfoType},
                        (proxy, method, args) -> {
                            if (method.getName().equals("getId")) {
                                return id;
                            }
                            return null;
                        });
    }
}
