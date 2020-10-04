/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import java.util.function.Function;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;

/**
 * {@link ResolvingCatalogFacade#setObjectResolver resolving function} that returns the incoming
 * object decorated with a {@link ModificationProxy}
 *
 * @see ModificationProxy#create(Object, Class)
 */
public class ModificationProxyDecorator {

    public static Function<CatalogInfo, CatalogInfo> wrap() {
        return ModificationProxyDecorator::wrap;
    }

    public static Function<CatalogInfo, CatalogInfo> unwrap() {
        return ModificationProxyDecorator::unwrap;
    }

    static CatalogInfo wrap(CatalogInfo i) {
        if (i != null) {
            ClassMappings mappings = ClassMappings.fromImpl(i.getClass());
            if (mappings == null) {
                throw new IllegalArgumentException(
                        "Can't determine CatalogInfo subtype, make sure the provided object is not a proxy: "
                                + i);
            }
            i = ModificationProxy.create(i, mappings.getInterface());
        }
        return i;
    }

    static CatalogInfo unwrap(CatalogInfo i) {
        return i == null ? null : ModificationProxy.unwrap(i);
    }
}
