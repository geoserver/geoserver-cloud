/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event.catalog;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.cloud.bus.event.RemoteAddEvent;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@EqualsAndHashCode(callSuper = true)
public class RemoteCatalogAddEvent extends RemoteAddEvent<Catalog, CatalogInfo>
        implements RemoteCatalogEvent {

    private static final long serialVersionUID = 1L;

    protected RemoteCatalogAddEvent() {
        // default constructor, needed for deserialization
    }

    public RemoteCatalogAddEvent(
            @NonNull Catalog source,
            @NonNull CatalogInfo object,
            @NonNull String originService,
            String destinationService) {
        super(source, object, originService, destinationService);
    }
}
