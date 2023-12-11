/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import lombok.experimental.UtilityClass;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ProxyUtils;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;

import java.util.function.Function;

/**
 * {@link ResolvingCatalogFacadeDecorator#setObjectResolver resolving function} that returns the
 * incoming object decorated with a {@link ModificationProxy}
 *
 * @see ModificationProxy#create(Object, Class)
 */
@UtilityClass
public class ModificationProxyDecorator {

    public static Function<CatalogInfo, CatalogInfo> wrap() {
        return ModificationProxyDecorator::wrap;
    }

    public static Function<CatalogInfo, CatalogInfo> unwrap() {
        return ModificationProxyDecorator::unwrap;
    }

    public static CatalogInfo wrap(CatalogInfo info) {
        if (info != null && null == ProxyUtils.handler(info, ModificationProxy.class)) {
            ClassMappings mappings = ClassMappings.fromImpl(info.getClass());
            if (mappings == null) {
                throw new IllegalArgumentException(
                        "Can't determine CatalogInfo subtype, make sure the provided object is not a proxy: %s"
                                .formatted(info));
            }
            @SuppressWarnings("unchecked")
            Class<? extends CatalogInfo> type =
                    (Class<? extends CatalogInfo>) mappings.getInterface();
            info = ModificationProxy.create(info, type);
        }
        return info;
    }

    public static CatalogInfo unwrap(CatalogInfo i) {
        return i == null ? null : ModificationProxy.unwrap(i);
    }
}
