/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import java.util.function.Function;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;

/**
 * {@link ExtendedCatalogFacade} extension that applies a possibly side-effect producing
 * {@link Function} to each {@link CatalogInfo} right before returning it.
 *
 * <p>
 * By default the function applied is the {@link Function#identity() identity} function, use
 * {@link #setOutboundResolver} to establish the function to apply to each object before being
 * returned.
 *
 * <p>
 * The function must accept {@code null} as argument. This {@link CatalogFacade} decorator does not
 * assume any special treatment for {@code null} objects, leaving the supplied resolving function
 * chain the freedom to return {@code null} or resolve to any other object.
 * <p>
 * Use function chaining to compose a resolving pipeline adequate to the {@link CatalogFacade}
 * implementation. For example, the following chain is appropriate for a raw catalog facade that
 * fetches new objects from a remote service, and where all {@link CatalogInfo} object references
 * (e.g. {@link StoreInfo#getWorkspace()}, etc.) are {@link ResolvingProxy} instances:
 *
 * <pre>
 * {@code
 * Catalog catalog = ...
 * Function<CatalogInfo, CatalogInfo> resolvingFunction;
 * resolvingFunction =
 *   CatalogPropertyResolver.of(catalog)
 *   .andThen(ResolvingProxyResolver.of(catalog)
 *   .andThen(CollectionPropertiesInitializer.instance())
 *   .andThen(ModificationProxyDecorator.wrap());
 *
 * ResolvingCatalogFacade facade = ...
 * facade.setOutboundResolver(resolvingFunction);
 * facade.setInboundResolver(ModificationProxyDecorator.unwrap());
 * }
 *
 * Will first set the catalog property if the object type requires it (e.g.
 * {@link ResourceInfo#setCatalog}), then resolve all {@link ResolvingProxy} proxied references,
 * then initialize collection properties that are {@code null} to empty collections, and finally
 * decorate the object with a {@link ModificationProxy}.
 * <p>
 * Note the caller is responsible of supplying a resolving function that utilizes the correct
 * {@link Catalog}, may some of the functions in the chain require one; {@link #setOutboundResolver}
 * is agnostic of such concerns.
 */
public interface ResolvingCatalogFacade extends ExtendedCatalogFacade, ResolvingFacade<CatalogInfo> {}
