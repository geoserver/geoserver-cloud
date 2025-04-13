/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.event.catalog;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Objects;
import java.util.Optional;
import lombok.NonNull;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.cloud.event.info.InfoRemoved;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("CatalogInfoRemoved")
@SuppressWarnings("serial")
public class CatalogInfoRemoved extends InfoRemoved {

    protected CatalogInfoRemoved() {}

    CatalogInfoRemoved(long updateSequence, @NonNull CatalogInfo info) {
        super(updateSequence, resolveId(info), prefixedName(info), typeOf(info));
    }

    @Override
    @NonNull
    public String getObjectName() {
        return Objects.requireNonNull(super.getObjectName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<CatalogInfoRemoved> remote() {
        return super.remote();
    }

    public static CatalogInfoRemoved createLocal(long updateSequence, @NonNull CatalogInfo info) {
        return new CatalogInfoRemoved(updateSequence, info);
    }
}
