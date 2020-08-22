/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.bus.catalog;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

@EqualsAndHashCode(callSuper = true)
public abstract class CatalogRemoteEvent extends RemoteApplicationEvent {
    private static final long serialVersionUID = 1L;

    /** Identifier of the catalog object this event refers to, from {@link CatalogInfo#getId()} */
    private @Getter String catalogInfoId;

    private @Getter ClassMappings catalogInfoEnumType;

    protected CatalogRemoteEvent() {
        // default constructor, needed for deserialization
    }

    protected CatalogRemoteEvent(
            Object source,
            String originService,
            String destinationService,
            @NonNull String catalogInfoId,
            @NonNull ClassMappings catalogInfoEnumType) {
        super(source, originService, destinationService);
        this.catalogInfoId = catalogInfoId;
        this.catalogInfoEnumType = catalogInfoEnumType;
    }

    public @Override String toString() {
        return String.format(
                "%s(%s: %s)[event id: %s, from: %s, to: %s, ts: %d]",
                getClass().getSimpleName(),
                this.getCatalogInfoEnumType(),
                this.getCatalogInfoId(),
                super.getId(),
                super.getOriginService(),
                super.getDestinationService(),
                super.getTimestamp());
    }

    public static @FunctionalInterface interface CatalogRemoteEventFactory {
        CatalogRemoteEvent create(
                Object source,
                String originService,
                String destinationService,
                @NonNull String catalogInfoId,
                @NonNull ClassMappings catalogInfoEnumType);
    }
}
