/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.bus;

import lombok.NonNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.resolving.CatalogPropertyResolver;
import org.geoserver.catalog.plugin.resolving.CollectionPropertiesInitializer;
import org.geoserver.catalog.plugin.resolving.ProxyUtils;
import org.geoserver.catalog.plugin.resolving.ResolvingProxyResolver;
import org.geoserver.cloud.event.info.InfoAdded;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.cloud.event.info.InfoModified;
import org.geoserver.config.GeoServer;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Highest priority listener for incoming {@link RemoteGeoServerEvent} events to resolve the payload
 * {@link CatalogInfo} properties, as they may come either as {@link ResolvingProxy} proxies, or
 * {@code null} in case of collection properties.
 *
 * <p>This listener ensures the payload object properties are resolved before being catch up by
 * other listeners.
 */
public class InfoEventResolver {

    private UnaryOperator<Info> configInfoResolver;
    private UnaryOperator<CatalogInfo> catalogInfoResolver;
    // REVISIT: merge ProxyUtils with ResolvingProxyResolver
    private ProxyUtils proxyUtils;

    public InfoEventResolver(@NonNull Catalog rawCatalog, @NonNull GeoServer geoserverConfig) {

        proxyUtils = new ProxyUtils(() -> rawCatalog, Optional.of(geoserverConfig));

        configInfoResolver =
                CollectionPropertiesInitializer.<Info>instance()
                                .andThen(ResolvingProxyResolver.<Info>of(rawCatalog))
                        ::apply;

        catalogInfoResolver =
                CollectionPropertiesInitializer.<CatalogInfo>instance()
                                .andThen(CatalogPropertyResolver.of(rawCatalog))
                                .andThen(ResolvingProxyResolver.of(rawCatalog))
                        ::apply;
    }

    @SuppressWarnings("unchecked")
    public InfoEvent resolve(InfoEvent event) {
        if (event instanceof InfoAdded addEvent) {
            Info object = addEvent.getObject();
            addEvent.setObject(resolveInfo(object));
        } else if (event instanceof InfoModified modifyEvent) {
            modifyEvent.setPatch(resolvePatch(modifyEvent.getPatch()));
        }
        return event;
    }

    @SuppressWarnings("unchecked")
    private <I extends Info> I resolveInfo(I object) {
        if (object == null) return null;
        if (object instanceof CatalogInfo i) {
            return (I) resolveCatalogInfo(i);
        }
        return (I) configInfoResolver.apply(object);
    }

    @SuppressWarnings("unchecked")
    private <C extends CatalogInfo> C resolveCatalogInfo(C object) {
        if (object == null) return null;
        return (C) catalogInfoResolver.apply(object);
    }

    private Patch resolvePatch(Patch patch) {
        return proxyUtils.resolve(patch);
    }
}
