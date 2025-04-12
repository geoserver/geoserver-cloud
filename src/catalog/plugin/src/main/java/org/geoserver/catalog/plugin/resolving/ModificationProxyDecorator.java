/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.resolving;

import java.util.function.UnaryOperator;
import lombok.experimental.UtilityClass;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ProxyUtils;

/**
 * A utility class providing {@link UnaryOperator} functions to wrap or unwrap objects with a
 * {@link ModificationProxy} for use in resolving facades.
 *
 * <p>This class offers factory methods to create operators that either decorate objects with a
 * {@link ModificationProxy} ({@link #wrap()}) or remove such proxies ({@link #unwrap()}). These operators
 * are designed for use with {@link ResolvingCatalogFacade#setOutboundResolver(UnaryOperator)} or
 * {@link ResolvingCatalogFacade#setInboundResolver(UnaryOperator)}, enabling modification tracking or
 * proxy removal in catalog operations. The wrapping preserves object state changes, while unwrapping
 * retrieves the underlying instance.
 *
 * <p>Key features:
 * <ul>
 *   <li><strong>Proxy Wrapping:</strong> {@link #wrap()} adds a {@link ModificationProxy} to objects if
 *       not already proxied, ensuring type safety via {@link ClassMappings}.</li>
 *   <li><strong>Proxy Unwrapping:</strong> {@link #unwrap()} removes {@link ModificationProxy} wrappers,
 *       returning the raw object or null if the input is null.</li>
 *   <li><strong>Null Safety:</strong> Both operators handle null inputs gracefully, returning null as
 *       appropriate.</li>
 * </ul>
 *
 * <p>Example usage in a resolving facade:
 * <pre>
 * {@code
 * ResolvingCatalogFacade facade = ...;
 * facade.setOutboundResolver(ModificationProxyDecorator.wrap());
 * facade.setInboundResolver(ModificationProxyDecorator.unwrap());
 * }
 * </pre>
 * This configures the facade to wrap outbound {@link CatalogInfo} objects in a {@link ModificationProxy}
 * and unwrap inbound objects.
 *
 * @since 1.0
 * @see ModificationProxy
 * @see ResolvingCatalogFacade
 */
@UtilityClass
public class ModificationProxyDecorator {

    /**
     * Returns a {@link UnaryOperator} that wraps objects in a {@link ModificationProxy}.
     *
     * <p>The operator applies {@link #wrap(Object)} to each input, adding a proxy if the object isn’t
     * already proxied, or returning it unchanged if it is or if null.
     *
     * @param <T> The type of object to wrap (typically a {@link CatalogInfo} subtype).
     * @return A {@link UnaryOperator} for wrapping objects; never null.
     * @example Using the wrap operator:
     *          <pre>
     *          UnaryOperator<CatalogInfo> wrapper = ModificationProxyDecorator.wrap();
     *          CatalogInfo info = ...;
     *          CatalogInfo proxied = wrapper.apply(info);
     *          </pre>
     */
    public static <T> UnaryOperator<T> wrap() {
        return ModificationProxyDecorator::wrap;
    }

    /**
     * Returns a {@link UnaryOperator} that unwraps objects from a {@link ModificationProxy}.
     *
     * <p>The operator applies {@link #unwrap(Object)} to each input, removing any {@link ModificationProxy}
     * wrapper or returning null if the input is null.
     *
     * @param <T> The type of object to unwrap (typically a {@link CatalogInfo} subtype).
     * @return A {@link UnaryOperator} for unwrapping objects; never null.
     * @example Using the unwrap operator:
     *          <pre>
     *          UnaryOperator<CatalogInfo> unwrapper = ModificationProxyDecorator.unwrap();
     *          CatalogInfo proxied = ...;
     *          CatalogInfo raw = unwrapper.apply(proxied);
     *          </pre>
     */
    public static <T> UnaryOperator<T> unwrap() {
        return ModificationProxyDecorator::unwrap;
    }

    /**
     * Wraps an object in a {@link ModificationProxy} if it isn’t already proxied.
     *
     * <p>Checks if the input is non-null and not already a {@link ModificationProxy}. If so, it determines
     * the appropriate catalog interface via {@link ClassMappings} and creates a proxy using
     * {@link ModificationProxy#create(Object, Class)}. Returns null if the input is null, or the original
     * object if already proxied.
     *
     * @param <T>  The type of object to wrap (typically a {@link CatalogInfo} subtype).
     * @param info The object to wrap; may be null.
     * @return The wrapped object, or null if {@code info} is null.
     * @throws IllegalArgumentException if {@code info} is non-null but its type cannot be mapped to a
     *                                  {@link CatalogInfo} subtype (e.g., an unrecognized proxy).
     */
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

    /**
     * Unwraps an object from a {@link ModificationProxy}, returning the underlying instance.
     *
     * <p>If the input is null, returns null. Otherwise, uses {@link ModificationProxy#unwrap(Object)} to
     * remove any proxy wrapper and return the raw object.
     *
     * @param <T> The type of object to unwrap (typically a {@link CatalogInfo} subtype).
     * @param i   The object to unwrap; may be null.
     * @return The unwrapped object, or null if {@code i} is null.
     */
    public static <T> T unwrap(T i) {
        return i == null ? null : ModificationProxy.unwrap(i);
    }
}
