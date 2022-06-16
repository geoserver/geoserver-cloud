/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.bus.catalog;

import lombok.NonNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.resolving.CatalogPropertyResolver;
import org.geoserver.catalog.plugin.resolving.CollectionPropertiesInitializer;
import org.geoserver.catalog.plugin.resolving.ResolvingProxyResolver;
import org.geoserver.cloud.event.info.InfoAddEvent;
import org.geoserver.cloud.event.info.InfoEvent;
import org.geoserver.cloud.event.info.InfoModifyEvent;
import org.geoserver.config.GeoServer;
import org.geoserver.jackson.databind.catalog.ProxyUtils;

import java.util.function.Function;

/**
 * Highest priority listener for incoming {@link RemoteInfoEvent} events to resolve the payload
 * {@link CatalogInfo} properties, as they may come either as {@link ResolvingProxy} proxies, or
 * {@code null} in case of collection properties.
 *
 * <p>This listener ensures the payload object properties are resolved before being catch up by
 * other listeners.
 */
public class InfoEventResolver {

    private final Catalog rawCatalog;
    private final GeoServer geoserverConfig;

    private Function<Info, Info> configInfoResolver;
    private Function<CatalogInfo, CatalogInfo> catalogInfoResolver;
    // REVISIT: merge ProxyUtils with ResolvingProxyResolver
    private ProxyUtils proxyUtils;

    public InfoEventResolver(@NonNull Catalog rawCatalog, @NonNull GeoServer geoserverConfig) {
        this.rawCatalog = rawCatalog;
        this.geoserverConfig = geoserverConfig;
        proxyUtils = new ProxyUtils(rawCatalog, geoserverConfig);

        configInfoResolver =
                CollectionPropertiesInitializer.<Info>instance()
                        .andThen(ResolvingProxyResolver.<Info>of(rawCatalog));

        catalogInfoResolver =
                CollectionPropertiesInitializer.<CatalogInfo>instance()
                        .andThen(CatalogPropertyResolver.of(rawCatalog))
                        .andThen(ResolvingProxyResolver.of(rawCatalog));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public InfoEvent<?, ?> resolve(InfoEvent event) {
        if (event instanceof InfoAddEvent) {
            InfoAddEvent addEvent = (InfoAddEvent) event;
            Info object = addEvent.getObject();
            addEvent.setObject(resolve(object));
        } else if (event instanceof InfoModifyEvent) {
            InfoModifyEvent modifyEvent = (InfoModifyEvent) event;
            modifyEvent.setPatch(resolve(modifyEvent.getPatch()));
        }
        return event;
    }

    @SuppressWarnings("unchecked")
    private <I extends Info> I resolve(I object) {
        if (object == null) return null;
        if (object instanceof CatalogInfo) {
            return (I) resolve((CatalogInfo) object);
        }
        return (I) configInfoResolver.apply(object);
    }

    private CatalogInfo resolve(CatalogInfo object) {
        if (object == null) return null;
        return catalogInfoResolver.apply(object);
    }

    private Patch resolve(Patch patch) {
        return proxyUtils.resolve(patch);
    }
}
