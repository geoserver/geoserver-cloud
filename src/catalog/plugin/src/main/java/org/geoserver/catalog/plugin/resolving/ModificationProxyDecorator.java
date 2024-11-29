/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import java.util.function.UnaryOperator;
import lombok.experimental.UtilityClass;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ProxyUtils;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;

/**
 * {@link ResolvingCatalogFacadeDecorator#setObjectResolver resolving function} that returns the
 * incoming object decorated with a {@link ModificationProxy}
 *
 * @see ModificationProxy#create(Object, Class)
 */
@UtilityClass
public class ModificationProxyDecorator {

    public static <T> UnaryOperator<T> wrap() {
        return ModificationProxyDecorator::wrap;
    }

    public static <T> UnaryOperator<T> unwrap() {
        return ModificationProxyDecorator::unwrap;
    }

    @SuppressWarnings("unchecked")
    public static <T> T wrap(T info) {
        if (info != null && null == ProxyUtils.handler(info, ModificationProxy.class)) {
            ClassMappings mappings = ClassMappings.fromImpl(info.getClass());
            if (mappings == null) {
                throw new IllegalArgumentException(
                        "Can't determine CatalogInfo subtype, make sure the provided object is not a proxy: %s"
                                .formatted(info));
            }
            Class<? extends Info> type = mappings.getInterface();
            info = (T) ModificationProxy.create(info, type);
        }
        return info;
    }

    public static <T> T unwrap(T i) {
        return i == null ? null : ModificationProxy.unwrap(i);
    }
}
