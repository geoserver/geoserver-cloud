/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event.catalog;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.cloud.bus.event.RemoteRemoveEvent;
import org.geoserver.cloud.event.ConfigInfoInfoType;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@EqualsAndHashCode(callSuper = true)
public class RemoteCatalogInfoRemoveEvent extends RemoteRemoveEvent<Catalog, CatalogInfo>
        implements RemoteCatalogEvent {

    private static final long serialVersionUID = 1L;

    private @Getter @Setter CatalogInfo object;

    /** default constructor, needed for deserialization */
    protected RemoteCatalogInfoRemoveEvent() {}

    /** Throwing constructor */
    public RemoteCatalogInfoRemoveEvent(
            @NonNull Catalog source,
            @NonNull CatalogInfo object,
            @NonNull String originService,
            String destinationService) {
        super(
                source,
                RemoteCatalogEvent.resolveId(object),
                ConfigInfoInfoType.valueOf(object),
                originService,
                destinationService);
        if (!(object instanceof Catalog)) {
            this.object = object;
        }
    }
}
