/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import java.util.function.Function;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;

/**
 * An extension of {@link ExtendedCatalogFacade} that incorporates resolving capabilities for
 * {@link CatalogInfo} objects, applying configurable transformation functions on inbound and outbound data.
 *
 * <p>The primary purpose of this interface is to simplify the implementation of concrete
 * {@link ExtendedCatalogFacade} classes by allowing them to act as plain Data Access Object (DAO)
 * interfaces, focusing solely on basic CRUD operations. It achieves this by delegating the resolution of
 * inbound and outbound final states of {@link CatalogInfo} objects to composable {@link Function} pipelines.
 * This separation enables facade implementors to handle raw data access while outsourcing complex object
 * transformations (e.g., proxy resolution, property initialization) to configurable resolvers.
 *
 * <p>This interface combines the advanced catalog operations of {@link ExtendedCatalogFacade} with the
 * resolving framework of {@link ResolvingFacade}, allowing implementations to transform {@link CatalogInfo}
 * objects both when received (inbound) and before they are returned (outbound). The outbound resolver,
 * configurable via {@link #setOutboundResolver(Function)}, applies a transformation function to each object
 * before it leaves the facade, while the inbound resolver, set via {@link #setInboundResolver(Function)},
 * processes incoming objects before they are handled by the underlying facade. By default, both resolvers
 * use the {@link Function#identity() identity} function, meaning no transformation occurs unless explicitly
 * configured.
 *
 * <p>Key features:
 * <ul>
 *   <li><strong>Simplified DAO Role:</strong> Enables facade implementations to focus on data access,
 *       leaving resolution logic to external functions.</li>
 *   <li><strong>Resolution Pipeline:</strong> Supports custom inbound and outbound transformations (e.g.,
 *       resolving proxies, decorating objects) via chained functions.</li>
 *   <li><strong>Null Handling:</strong> Requires resolvers to accept null inputs, leaving null handling to
 *       the resolver’s discretion.</li>
 *   <li><strong>Extended Functionality:</strong> Inherits modern catalog operations from
 *       {@link ExtendedCatalogFacade}, such as stream-based querying and patch updates.</li>
 * </ul>
 *
 * <p>Implementations must apply {@link #resolveOutbound(CatalogInfo)} to all returned objects and
 * {@link #resolveInbound(CatalogInfo)} to all received objects, ensuring consistent transformation across
 * operations like {@link #add(CatalogInfo)}, {@link #get(String)}, and {@link #query(Query)}.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * Catalog catalog = ...;
 * Function<CatalogInfo, CatalogInfo> resolvingFunction =
 *     CatalogPropertyResolver.of(catalog)
 *         .andThen(ResolvingProxyResolver.of(catalog))
 *         .andThen(CollectionPropertiesInitializer.instance())
 *         .andThen(ModificationProxyDecorator.wrap());
 *
 * ResolvingCatalogFacade facade = ...;
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
 *   <li>The resolver functions must handle {@code null} inputs, allowing implementations to decide whether
 *       to propagate or transform nulls.</li>
 *   <li>Callers are responsible for ensuring resolver functions use the correct {@link Catalog} instance if
 *       required, as {@link #setOutboundResolver(Function)} and {@link #setInboundResolver(Function)} are
 *       agnostic to such dependencies.</li>
 * </ul>
 *
 * @since 1.0
 * @see ExtendedCatalogFacade
 * @see ResolvingFacade
 * @see CatalogInfo
 */
public interface ResolvingCatalogFacade extends ExtendedCatalogFacade, ResolvingFacade<CatalogInfo> {}
