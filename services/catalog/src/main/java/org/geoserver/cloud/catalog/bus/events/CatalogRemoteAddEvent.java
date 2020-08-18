package org.geoserver.cloud.catalog.bus.events;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.geoserver.catalog.impl.ClassMappings;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CatalogRemoteAddEvent extends CatalogRemoteEvent {
    private static final long serialVersionUID = 1L;

    protected CatalogRemoteAddEvent() {
        // default constructor, needed for deserialization
    }

    public CatalogRemoteAddEvent(
            Object source,
            String originService,
            String destinationService,
            @NonNull String catalogInfoId,
            @NonNull ClassMappings catalogInfoEnumType) {
        super(source, originService, destinationService, catalogInfoId, catalogInfoEnumType);
    }
}
