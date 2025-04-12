/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.resolving;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * A utility implementation of {@link ResolvingFacade} for managing inbound and outbound resolution of generic objects.
 *
 * <p>This class provides a concrete support mechanism for applying {@link UnaryOperator} functions to objects
 * of type {@code T}, both when received (inbound) and before they are returned (outbound). It maintains two
 * resolvers—{@code outboundResolver} and {@code inboundResolver}—defaulting to the identity function
 * ({@link UnaryOperator#identity()}), and allows customization via {@link #setOutboundResolver(UnaryOperator)}
 * and {@link #setInboundResolver(UnaryOperator)}. It’s designed to simplify the integration of resolution
 * logic into facades or repositories, such as {@link ResolvingCatalogFacadeDecorator}.
 *
 * <p>Key aspects:
 * <ul>
 *   <li><strong>Resolver Management:</strong> Holds and applies configurable inbound and outbound resolvers.</li>
 *   <li><strong>List Support:</strong> Includes a helper method to resolve lists of objects outbound.</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * <code>
 * ResolvingFacadeSupport<CatalogInfo> support = new ResolvingFacadeSupport<>();
 * support.setOutboundResolver(info -> { &lt;custom logic&gt;... return info; });
 * CatalogInfo resolved = support.resolveOutbound(info);
 * </code>
 * </pre>
 *
 * @param <T> The type of objects to resolve (e.g., {@link CatalogInfo}).
 * @since 1.0
 * @see ResolvingFacade
 * @see UnaryOperator
 */
public class ResolvingFacadeSupport<T> implements ResolvingFacade<T> {

    private UnaryOperator<T> outboundResolver = UnaryOperator.identity();
    private UnaryOperator<T> inboundResolver = UnaryOperator.identity();

    /**
     * {@inheritDoc}
     * @throws NullPointerException if {@code resolvingFunction} is null.
     */
    @Override
    public void setOutboundResolver(UnaryOperator<T> resolvingFunction) {
        Objects.requireNonNull(resolvingFunction, "Outbound resolving function must not be null");
        this.outboundResolver = resolvingFunction;
    }

    /**
     * {@inheritDoc}
     * <p>Returns the current outbound resolver, defaulting to {@link UnaryOperator#identity()}.
     */
    @Override
    public UnaryOperator<T> getOutboundResolver() {
        return this.outboundResolver;
    }

    /**
     * {@inheritDoc}
     * @throws NullPointerException if {@code resolvingFunction} is null.
     */
    @Override
    public void setInboundResolver(UnaryOperator<T> resolvingFunction) {
        Objects.requireNonNull(resolvingFunction, "Inbound resolving function must not be null");
        this.inboundResolver = resolvingFunction;
    }

    /**
     * {@inheritDoc}
     * <p>Returns the current inbound resolver, defaulting to {@link UnaryOperator#identity()}.
     */
    @Override
    public UnaryOperator<T> getInboundResolver() {
        return this.inboundResolver;
    }

    /**
     * Provides the outbound resolver as a type-safe {@link UnaryOperator} for a specific subtype.
     *
     * @param <I> The subtype of {@code T}.
     * @return The outbound resolver cast to the subtype; never null.
     */
    @SuppressWarnings("unchecked")
    public <I extends T> UnaryOperator<I> outbound() {
        return (UnaryOperator<I>) outboundResolver;
    }

    /**
     * Provides the inbound resolver as a type-safe {@link UnaryOperator} for a specific subtype.
     *
     * @param <I> The subtype of {@code T}.
     * @return The inbound resolver cast to the subtype; never null.
     */
    @SuppressWarnings("unchecked")
    public <I extends T> UnaryOperator<I> inbound() {
        return (UnaryOperator<I>) inboundResolver;
    }

    /**
     * {@inheritDoc}
     * <p>Applies the outbound resolver to the input object.
     */
    @Override
    public <C extends T> C resolveOutbound(C info) {
        UnaryOperator<C> outboundResolve = outbound();
        return outboundResolve.apply(info);
    }

    /**
     * {@inheritDoc}
     * <p>Applies the inbound resolver to the input object.
     */
    @Override
    public <C extends T> C resolveInbound(C info) {
        UnaryOperator<C> inboundResolve = inbound();
        return inboundResolve.apply(info);
    }

    /**
     * Resolves a list of objects using the outbound resolver.
     *
     * <p>Transforms each element in the list via {@link #resolveOutbound(Object)}, preserving order and
     * allowing null results.
     *
     * @param <C>  The subtype of {@code T}.
     * @param info The list to resolve; must not be null.
     * @return A new list with resolved elements.
     * @throws NullPointerException if {@code info} is null.
     */
    protected <C extends T> List<C> resolveOutbound(List<C> info) {
        return Lists.transform(info, this::resolveOutbound);
    }
}
