/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.resolving;

import java.util.function.UnaryOperator;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResolvingProxy;

/**
 * Interface that defines methods for applying resolution functions to objects of type {@code T}
 * both inbound and outbound, enabling customizable object transformation in a catalog context.
 *
 * <p>This interface provides a generic framework for processing objects (typically {@link CatalogInfo})
 * entering and exiting a facade, using configurable {@link UnaryOperator} functions. The outbound resolver
 * transforms objects before they are returned to the caller, while the inbound resolver processes objects
 * received from the caller before they are passed to the underlying implementation. By default, both
 * resolvers are set to the {@link UnaryOperator#identity() identity} function, meaning no transformation
 * occurs unless explicitly configured via {@link #setOutboundResolver(UnaryOperator)} or
 * {@link #setInboundResolver(UnaryOperator)}.
 *
 * <p>Key features:
 * <ul>
 *   <li><strong>Flexible Resolution:</strong> Supports custom inbound and outbound transformations, such
 *       as resolving proxies or initializing properties.</li>
 *   <li><strong>Null Safety:</strong> Requires resolvers to handle null inputs, allowing implementations
 *       to decide how to process or propagate nulls.</li>
 *   <li><strong>Pipeline Composition:</strong> Encourages chaining functions to build complex resolution
 *       workflows tailored to specific needs.</li>
 * </ul>
 *
 * <p>Implementations (e.g., catalog facades) must apply {@link #resolveOutbound(T)} to every object returned
 * and {@link #resolveInbound(T)} to every object received, ensuring consistent transformation across all
 * operations.
 *
 * <p>Example usage for a catalog facade:
 * <pre>
 * {@code
 * Catalog catalog = ...;
 * UnaryOperator<CatalogInfo> resolvingFunction =
 *     CatalogPropertyResolver.of(catalog)
 *         .andThen(ResolvingProxyResolver.of(catalog))
 *         .andThen(CollectionPropertiesInitializer.instance())
 *         .andThen(ModificationProxyDecorator.wrap());
 *
 * ResolvingFacade<CatalogInfo> facade = ...;
 * facade.setOutboundResolver(resolvingFunction);
 * facade.setInboundResolver(ModificationProxyDecorator.unwrap());
 * }
 * </pre>
 * This pipeline sets the catalog property (e.g., {@link ResourceInfo#setCatalog}), resolves
 * {@link ResolvingProxy} references, initializes null collections, and wraps objects in a
 * {@link ModificationProxy} for outbound objects, while unwrapping proxies for inbound objects.
 *
 * <p>Notes:
 * <ul>
 *   <li>Resolvers must accept {@code null} as an argument and may return {@code null}, giving
 *       implementations flexibility in handling missing or unresolved objects.</li>
 *   <li>Callers must ensure resolvers use the correct {@link Catalog} instance if required by the
 *       chained functions, as this interface remains agnostic to such dependencies.</li>
 * </ul>
 *
 * @param <T> The type of objects to resolve (typically {@link CatalogInfo} or a subtype).
 * @since 1.0
 * @see CatalogInfo
 * @see ResolvingProxyResolver
 * @see ModificationProxyDecorator
 */
public interface ResolvingFacade<T> {

    /**
     * Sets the resolver function applied to all outgoing objects before they are returned by the facade.
     *
     * <p>This function transforms objects of type {@code T} right before they leave the facade, enabling
     * custom processing such as resolving references or adding properties. The default is
     * {@link UnaryOperator#identity()}.
     *
     * @param resolvingFunction The {@link UnaryOperator} to apply to outbound objects; must not be null and
     *                          must handle null inputs.
     * @throws NullPointerException if {@code resolvingFunction} is null.
     * @example Setting an outbound resolver:
     *          <pre>
     *          ResolvingFacade<CatalogInfo> facade = ...;
     *          UnaryOperator<CatalogInfo> resolver = ModificationProxyDecorator.wrap();
     *          facade.setOutboundResolver(resolver);
     *          </pre>
     */
    void setOutboundResolver(UnaryOperator<T> resolvingFunction);

    /**
     * Retrieves the current outbound resolver function.
     *
     * <p>This function is applied to all objects of type {@code T} before they are returned by the facade’s
     * methods.
     *
     * @return The current outbound {@link UnaryOperator}; never null, defaults to
     *         {@link UnaryOperator#identity()}.
     * @example Chaining an additional resolver:
     *          <pre>
     *          ResolvingFacade<CatalogInfo> facade = ...;
     *          UnaryOperator<CatalogInfo> current = facade.getOutboundResolver();
     *          facade.setOutboundResolver(current.andThen(myCustomFilter));
     *          </pre>
     */
    UnaryOperator<T> getOutboundResolver();

    /**
     * Sets the resolver function applied to all incoming objects before they are processed by the facade.
     *
     * <p>This function transforms objects of type {@code T} received by the facade (e.g., for add or update
     * operations) before they are passed to the underlying implementation. The default is
     * {@link UnaryOperator#identity()}.
     *
     * @param resolvingFunction The {@link UnaryOperator} to apply to inbound objects; must not be null and
     *                          must handle null inputs.
     * @throws NullPointerException if {@code resolvingFunction} is null.
     * @example Setting an inbound resolver:
     *          <pre>
     *          ResolvingFacade<CatalogInfo> facade = ...;
     *          UnaryOperator<CatalogInfo> resolver = ModificationProxyDecorator.unwrap();
     *          facade.setInboundResolver(resolver);
     *          </pre>
     */
    void setInboundResolver(UnaryOperator<T> resolvingFunction);

    /**
     * Retrieves the current inbound resolver function.
     *
     * <p>This function is applied to all objects of type {@code T} entering the facade before they are
     * processed by its methods.
     *
     * @return The current inbound {@link UnaryOperator}; never null, defaults to
     *         {@link UnaryOperator#identity()}.
     * @example Chaining an additional inbound resolver:
     *          <pre>
     *          ResolvingFacade<CatalogInfo> facade = ...;
     *          UnaryOperator<CatalogInfo> current = facade.getInboundResolver();
     *          facade.setInboundResolver(current.andThen(myCustomValidator));
     *          </pre>
     */
    UnaryOperator<T> getInboundResolver();

    /**
     * Applies the outbound resolver to an object of type {@code T}.
     *
     * <p>Processes the object using the configured outbound resolver, which may transform it (e.g.,
     * resolving references) or return null if unresolved or filtered out. Implementations must call this
     * method on every object before returning it to the caller.
     *
     * @param <C>  The specific type of object (a subtype of {@code T}).
     * @param info The object to resolve; may be null.
     * @return The resolved object, or null if the resolver returns null.
     */
    <C extends T> C resolveOutbound(C info);

    /**
     * Applies the inbound resolver to an object of type {@code T}.
     *
     * <p>Processes the object using the configured inbound resolver, which may transform it (e.g.,
     * unwrapping proxies) or return null if invalid. Implementations must call this method on every object
     * received from the caller before further processing.
     *
     * @param <C>  The specific type of object (a subtype of {@code T}).
     * @param info The object to resolve; may be null.
     * @return The resolved object, or null if the resolver returns null.
     */
    <C extends T> C resolveInbound(C info);
}
