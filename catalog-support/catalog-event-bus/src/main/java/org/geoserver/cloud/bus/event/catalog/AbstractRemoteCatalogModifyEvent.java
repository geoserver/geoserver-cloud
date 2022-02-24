/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event.catalog;

import lombok.NonNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.bus.event.RemoteModifyEvent;
import org.geoserver.cloud.event.ConfigInfoInfoType;

public abstract class AbstractRemoteCatalogModifyEvent
        extends RemoteModifyEvent<Catalog, CatalogInfo> implements RemoteCatalogEvent {
    private static final long serialVersionUID = 1L;

    /** default constructor, needed for deserialization */
    protected AbstractRemoteCatalogModifyEvent() {
        //
    }

    public AbstractRemoteCatalogModifyEvent(
            @NonNull Catalog source,
            @NonNull CatalogInfo object,
            @NonNull Patch patch,
            @NonNull String originService,
            String destinationService) {
        super(
                source,
                RemoteCatalogEvent.resolveId(object),
                ConfigInfoInfoType.valueOf(object),
                patch,
                originService,
                destinationService);
    }
}
