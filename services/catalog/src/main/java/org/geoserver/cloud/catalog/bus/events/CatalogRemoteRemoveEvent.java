/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.catalog.bus.events;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.geoserver.catalog.impl.ClassMappings;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CatalogRemoteRemoveEvent extends CatalogRemoteEvent {
    private static final long serialVersionUID = 1L;

    /** default constructor, needed for deserialization */
    protected CatalogRemoteRemoveEvent() {}

    public CatalogRemoteRemoveEvent(
            Object source,
            String originService,
            String destinationService,
            @NonNull String catalogInfoId,
            @NonNull ClassMappings catalogInfoEnumType) {
        super(source, originService, destinationService, catalogInfoId, catalogInfoEnumType);
    }
}
