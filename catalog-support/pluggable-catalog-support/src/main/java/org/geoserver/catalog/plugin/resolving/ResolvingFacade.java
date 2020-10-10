/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.resolving;

import java.util.function.Function;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResolvingProxy;

/**
 * Facade trait that applies a possibly side-effect producing {@link Function} to each outgoing
 * object right before returning it, and an in-bound resolving function to each object reaching to
 * the facade.
 * <p>
 * Implementations are responsible of calling {@link #resolveOutbound} before returning every
 * object, and {@link #resolveInbound} on each received object.
 *
 * <p>
 * By default the function applied is the {@link Function#identity() identity} function, use
 * {@link #setOutboundResolver} to establish the function to apply to each object before being
 * returned.
 *
 * <p>
 * The function must accept {@code null} as argument.
 * <p>
 * Use function chaining to compose a resolving pipeline adequate to the implementation. For
 * example, the following chain is appropriate for a raw catalog facade that fetches new objects
 * from a remote service, and where all {@link CatalogInfo} object references (e.g.
 * {@link StoreInfo#getWorkspace()}, etc.) are {@link ResolvingProxy} instances:
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
public interface ResolvingFacade<T> {

    /**
     * Function applied to all outgoing {@link <T>} objects returned by the facade before leaving
     * the called method
     */
    void setOutboundResolver(Function<T, T> resolvingFunction);

    /**
     * Function applied to all incoming {@link <T>} objects before proceeding to execute the called
     * method
     *
     * <p>Use {@code facade.setOutboundResolver(facade.getOutboundResolver().andThen(myFunction))}
     * to add traits to the current resolver; for example, a filtering trait could be added this way
     * to filter out objects based on some externally defined conditions, returning {@code null} if
     * an object is to be discarded from the final outcome
     */
    Function<T, T> getOutboundResolver();

    /**
     * Function applied to all incoming {@link <T>} objects before deferring to the decorated facade
     */
    void setInboundResolver(Function<T, T> resolvingFunction);

    /**
     * Function applied to all incoming {@link <T>} objects before deferring to the decorated
     * facade.
     *
     * <p>Use {@code facade.setInboundResolver(facade.getInboundResolver().andThen(myFunction))} to
     * add traits to the current resolver
     */
    Function<T, T> getInboundResolver();

    <C extends T> C resolveOutbound(C info);

    <C extends T> C resolveInbound(C info);
}
