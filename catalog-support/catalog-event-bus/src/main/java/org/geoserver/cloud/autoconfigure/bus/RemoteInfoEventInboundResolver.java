/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.bus;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

import java.util.function.Function;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.resolving.CatalogPropertyResolver;
import org.geoserver.catalog.plugin.resolving.CollectionPropertiesInitializer;
import org.geoserver.catalog.plugin.resolving.ResolvingProxyResolver;
import org.geoserver.cloud.bus.event.RemoteAddEvent;
import org.geoserver.cloud.bus.event.RemoteInfoEvent;
import org.geoserver.cloud.bus.event.RemoteModifyEvent;
import org.geoserver.cloud.bus.event.RemoteRemoveEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

/**
 * Highest priority listener for incoming {@link RemoteInfoEvent} events to resolve the payload
 * {@link CatalogInfo} properties, as they may come either as {@link ResolvingProxy} proxies, or
 * {@code null} in case of collection properties.
 *
 * <p>This listener ensures the payload object properties are resolved before being catch up by
 * other listeners.
 */
public class RemoteInfoEventInboundResolver {

    private @Autowired ServiceMatcher busServiceMatcher;
    private Function<Info, Info> configInfoResolver;
    private Function<CatalogInfo, CatalogInfo> catalogInfoResolver;

    public @Autowired void setCatalog(@Qualifier("rawCatalog") Catalog rawCatalog) {
        configInfoResolver =
                CollectionPropertiesInitializer.<Info>instance()
                        .andThen(ResolvingProxyResolver.<Info>of(rawCatalog));

        catalogInfoResolver =
                CollectionPropertiesInitializer.<CatalogInfo>instance()
                        .andThen(CatalogPropertyResolver.of(rawCatalog))
                        .andThen(ResolvingProxyResolver.of(rawCatalog));
    }

    @Order(HIGHEST_PRECEDENCE)
    @EventListener(RemoteInfoEvent.class)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void resolve(RemoteInfoEvent<?, ?> event) {
        boolean isIncomingRemoteEvent = !busServiceMatcher.isFromSelf(event);
        if (isIncomingRemoteEvent) {
            event.setBusServiceMatcher(busServiceMatcher);
            if (event instanceof RemoteAddEvent) {
                RemoteAddEvent addEvent = (RemoteAddEvent) event;
                addEvent.setObject(resolve(addEvent.getObject()));
            }
            if (event instanceof RemoteRemoveEvent) {
                RemoteRemoveEvent removeEvent = (RemoteRemoveEvent) event;
                removeEvent.setObject(resolve(removeEvent.getObject()));
            }
            if (event instanceof RemoteModifyEvent) {
                RemoteModifyEvent modifyEvent = (RemoteModifyEvent) event;
                modifyEvent.setPatch(resolve(modifyEvent.getPatch()));
            }
        }
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
        if (patch == null || patch.isEmpty()) return patch;

        Patch resolved = new Patch();
        patch.getPatches().forEach(p -> resolved.add(p.withValue(resolvePatchValue(p.getValue()))));
        return resolved;
    }

    private Object resolvePatchValue(Object value) {
        if (value instanceof Info) {
            value = resolve((Info) value);
        }
        return value;
    }
}
