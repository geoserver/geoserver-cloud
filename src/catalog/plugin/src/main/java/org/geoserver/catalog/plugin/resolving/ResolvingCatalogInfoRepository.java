/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.resolving;

import java.util.function.UnaryOperator;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository;

/**
 * An abstract base class for catalog info repositories that support resolving {@link CatalogInfo} objects
 * with configurable inbound and outbound transformations.
 *
 * <p>This class combines the data access functionality of {@link CatalogInfoRepository} with the resolving
 * capabilities of {@link ResolvingFacade}, enabling concrete implementations to manage {@link CatalogInfo}
 * persistence while applying custom transformations (e.g., proxy resolution, property initialization) to
 * objects entering and leaving the repository. It uses a {@link ResolvingFacadeSupport} instance to handle
 * the resolution logic, with default identity resolvers that can be customized via
 * {@link #setOutboundResolver(UnaryOperator)} and {@link #setInboundResolver(UnaryOperator)}.
 *
 * <p>Key aspects:
 * <ul>
 *   <li><strong>Resolution Support:</strong> Provides methods to set and apply inbound/outbound resolvers
 *       for {@link CatalogInfo} objects.</li>
 *   <li><strong>Repository Base:</strong> Serves as a foundation for type-specific repositories (e.g.,
 *       for {@link org.geoserver.catalog.StoreInfo}), delegating CRUD operations to subclasses.</li>
 * </ul>
 *
 * <p>Subclasses must implement the {@link CatalogInfoRepository} methods (e.g., {@code add}, {@code findById})
 * and ensure resolved objects are processed appropriately using {@link #resolveInbound(CatalogInfo)} for
 * inputs and {@link #resolveOutbound(CatalogInfo)} for outputs.
 *
 * <p>Example usage:
 * <pre>
 * ResolvingCatalogInfoRepository<StoreInfo> repo = ...;
 * repo.setOutboundResolver(CatalogPropertyResolver.of(catalog));
 * StoreInfo store = repo.findById("id", StoreInfo.class); // Returns resolved object
 * </pre>
 *
 * @param <T> The specific type of {@link CatalogInfo} this repository manages.
 * @since 1.4
 * @see CatalogInfoRepository
 * @see ResolvingFacade
 * @see ResolvingFacadeSupport
 */
public abstract class ResolvingCatalogInfoRepository<T extends CatalogInfo>
        implements ResolvingFacade<T>, CatalogInfoRepository<T> {

    private final ResolvingFacadeSupport<T> resolver;

    /**
     * Constructs a new resolving repository with default identity resolvers.
     *
     * <p>Initializes the internal {@link ResolvingFacadeSupport} to manage inbound and outbound resolution
     * with no transformations unless explicitly set.
     */
    protected ResolvingCatalogInfoRepository() {
        this.resolver = new ResolvingFacadeSupport<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOutboundResolver(UnaryOperator<T> resolvingFunction) {
        resolver.setOutboundResolver(resolvingFunction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnaryOperator<T> getOutboundResolver() {
        return resolver.getOutboundResolver();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInboundResolver(UnaryOperator<T> resolvingFunction) {
        resolver.setInboundResolver(resolvingFunction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UnaryOperator<T> getInboundResolver() {
        return resolver.getInboundResolver();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C extends T> C resolveOutbound(C info) {
        return resolver.resolveOutbound(info);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <C extends T> C resolveInbound(C info) {
        return resolver.resolveInbound(info);
    }
}
