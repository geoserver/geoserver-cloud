/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.bus.catalog;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.geoserver.catalog.impl.ClassMappings;

@EqualsAndHashCode(callSuper = true)
public class CatalogRemoteModifyEvent extends CatalogRemoteEvent {
    private static final long serialVersionUID = 1L;

    /** default constructor, needed for deserialization */
    protected CatalogRemoteModifyEvent() {
        //
    }

    public CatalogRemoteModifyEvent(
            Object source,
            String originService,
            String destinationService,
            @NonNull String catalogInfoId,
            @NonNull ClassMappings catalogInfoEnumType) {
        super(source, originService, destinationService, catalogInfoId, catalogInfoEnumType);
    }
}
