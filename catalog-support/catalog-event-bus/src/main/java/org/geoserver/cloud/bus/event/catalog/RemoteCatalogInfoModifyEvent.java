/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.event.catalog;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.plugin.Patch;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@EqualsAndHashCode(callSuper = true)
public class RemoteCatalogInfoModifyEvent extends AbstractRemoteCatalogModifyEvent {
    private static final long serialVersionUID = 1L;

    /** default constructor, needed for deserialization */
    protected RemoteCatalogInfoModifyEvent() {
        //
    }

    public RemoteCatalogInfoModifyEvent(
            @NonNull Catalog source,
            @NonNull CatalogInfo object,
            @NonNull Patch patch,
            @NonNull String originService,
            String destinationService) {

        super(source, object, patch, originService, destinationService);
    }
}
