/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.event.catalog;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.NonNull;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.info.InfoRemoved;

import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("CatalogInfoRemoved")
@SuppressWarnings("serial")
public class CatalogInfoRemoved extends InfoRemoved {

    protected CatalogInfoRemoved() {}

    CatalogInfoRemoved(long updateSequence, @NonNull String id, @NonNull ConfigInfoType type) {
        super(updateSequence, id, type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CatalogInfoRemoved> remote() {
        return super.remote();
    }

    public static CatalogInfoRemoved createLocal(long updateSequence, @NonNull CatalogInfo info) {
        String id = resolveId(info);
        ConfigInfoType type = ConfigInfoType.valueOf(info);
        return new CatalogInfoRemoved(updateSequence, id, type);
    }
}
