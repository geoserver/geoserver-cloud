/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.catalog;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Optional;
import lombok.NonNull;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.cloud.event.info.InfoAdded;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("CatalogInfoAdded")
@SuppressWarnings("serial")
public class CatalogInfoAdded extends InfoAdded<CatalogInfo> {

    protected CatalogInfoAdded() {}

    CatalogInfoAdded(long updateSequence, @NonNull CatalogInfo object) {
        super(updateSequence, object);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CatalogInfoAdded> remote() {
        return super.remote();
    }

    public static CatalogInfoAdded createLocal(long updateSequence, @NonNull CatalogAddEvent event) {
        return createLocal(updateSequence, event.getSource());
    }

    public static CatalogInfoAdded createLocal(long updateSequence, @NonNull CatalogInfo info) {
        return new CatalogInfoAdded(updateSequence, info);
    }
}
