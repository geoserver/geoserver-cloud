/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.bus;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Ensures the {@link Info} and {@link Patch} object payloads on {@link InfoAdded} and {@link
 * InfoModified} events from a remote source get their properties resolved before being catch up by
 * other listeners.
 */
@Slf4j(topic = "org.geoserver.cloud.event.bus.resolve")
class InfoEventResolver {

    private UnaryOperator<Info> configInfoResolver;
    private UnaryOperator<CatalogInfo> catalogInfoResolver;
    // REVISIT: merge ProxyUtils with ResolvingProxyResolver
    private ProxyUtils proxyUtils;

    public InfoEventResolver(@NonNull Catalog rawCatalog, @NonNull GeoServer geoserverConfig) {

        proxyUtils = new ProxyUtils(() -> rawCatalog, Optional.of(geoserverConfig));

        BiConsumer<Info, ResolvingProxy> onNotFound = (info, proxy) -> log.debug(
                "Event object contains a reference to a non existing object ResolvingProxy(ref={})", proxy.getRef());

        configInfoResolver = CollectionPropertiesInitializer.<Info>instance()
                .andThen(ResolvingProxyResolver.<Info>of(rawCatalog).onNotFound(onNotFound))::apply;

        var catalogResolver = CatalogPropertyResolver.<CatalogInfo>of(rawCatalog);
        var resolvingProxyResolver =
                ResolvingProxyResolver.<CatalogInfo>of(rawCatalog).onNotFound(onNotFound);
        var collectionsInitializer = CollectionPropertiesInitializer.<CatalogInfo>instance();

        catalogInfoResolver = catalogResolver.andThen(collectionsInitializer).andThen(resolvingProxyResolver)::apply;
    }

    @SuppressWarnings("unchecked")
    public InfoEvent resolve(InfoEvent event) {
        if (event instanceof InfoAdded addEvent) {
            addEvent.setObject(resolveInfo(addEvent.getObject()));
        } else if (event instanceof InfoModified modifyEvent) {
            modifyEvent.setPatch(resolvePatch(modifyEvent.getPatch()));
        }
        return event;
    }

    private Info resolveInfo(Info object) {
        if (object instanceof CatalogInfo i) {
            return resolveCatalogInfo(i);
        }
        return object == null ? null : configInfoResolver.apply(object);
    }

    private CatalogInfo resolveCatalogInfo(CatalogInfo info) {
        return info == null ? null : catalogInfoResolver.apply(info);
    }

    private Patch resolvePatch(Patch patch) {
        return proxyUtils.resolve(patch);
    }
}
