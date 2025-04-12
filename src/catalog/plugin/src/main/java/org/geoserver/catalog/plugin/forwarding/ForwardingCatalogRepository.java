/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog.plugin.forwarding;

import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Query;
import org.geotools.api.filter.Filter;

/**
 * An abstract decorator for {@link CatalogInfoRepository} that forwards all method calls to an underlying repository.
 *
 * <p>This class simplifies the creation of repository decorators by delegating all operations to a subject
 * {@link CatalogInfoRepository}, enabling subclasses to override specific methods to customize behavior
 * (e.g., adding logging, validation, or caching). It’s designed for type-specific repositories managing
 * {@link CatalogInfo} subtypes.
 *
 * <p>Example usage:
 * <pre>
 * CatalogInfoRepository&lt;StoreInfo&gt; baseRepo = ...;
 * ForwardingCatalogRepository&lt;StoreInfo, CatalogInfoRepository&lt;StoreInfo&gt;&gt; decorator =
 *     new ForwardingCatalogRepository&lt;&gt;(baseRepo) {
 *         &#64;Override
 *         public void add(StoreInfo value) {
 *             // Custom logic before forwarding
 *             super.add(value);
 *         }
 *     };
 * </pre>
 *
 * @param <I> The type of {@link CatalogInfo} managed by this repository.
 * @param <S> The specific {@link CatalogInfoRepository} subtype being decorated.
 * @since 1.0
 * @see CatalogInfoRepository
 */
public abstract class ForwardingCatalogRepository<I extends CatalogInfo, S extends CatalogInfoRepository<I>>
        implements CatalogInfoRepository<I> {

    protected S subject;

    /**
     * Constructs a forwarding repository wrapping the provided subject.
     *
     * @param subject The underlying {@link CatalogInfoRepository} to forward calls to; may be null (behavior depends on subclass).
     */
    protected ForwardingCatalogRepository(S subject) {
        this.subject = subject;
    }

    /** {@inheritDoc} */
    @Override
    public Class<I> getContentType() {
        return subject.getContentType();
    }

    /** {@inheritDoc} */
    @Override
    public boolean canSortBy(@NonNull String propertyName) {
        return subject.canSortBy(propertyName);
    }

    /** {@inheritDoc} */
    @Override
    public void add(I value) {
        subject.add(value);
    }

    /** {@inheritDoc} */
    @Override
    public void remove(I value) {
        subject.remove(value);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends I> T update(T value, Patch patch) {
        return subject.update(value, patch);
    }

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        subject.dispose();
    }

    /** {@inheritDoc} */
    @Override
    public Stream<I> findAll() {
        return subject.findAll();
    }

    /** {@inheritDoc} */
    @Override
    public <U extends I> Stream<U> findAll(Query<U> query) {
        return subject.findAll(query);
    }

    /** {@inheritDoc} */
    @Override
    public <U extends I> long count(final Class<U> of, final Filter filter) {
        return subject.count(of, filter);
    }

    /** {@inheritDoc} */
    @Override
    public <U extends I> Optional<U> findById(String id, Class<U> clazz) {
        return subject.findById(id, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public <U extends I> Optional<U> findFirstByName(@NonNull String name, Class<U> clazz) {
        return subject.findFirstByName(name, clazz);
    }

    /** {@inheritDoc} */
    @Override
    public void syncTo(CatalogInfoRepository<I> target) {
        subject.syncTo(target);
    }
}
