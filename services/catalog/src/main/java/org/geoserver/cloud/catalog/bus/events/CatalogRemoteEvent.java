package org.geoserver.cloud.catalog.bus.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
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
}
