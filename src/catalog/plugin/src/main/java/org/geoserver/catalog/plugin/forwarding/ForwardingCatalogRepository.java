/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
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

public abstract class ForwardingCatalogRepository<I extends CatalogInfo, S extends CatalogInfoRepository<I>>
        implements CatalogInfoRepository<I> {

    protected S subject;

    protected ForwardingCatalogRepository(S subject) {
        this.subject = subject;
    }

    @Override
    public Class<I> getContentType() {
        return subject.getContentType();
    }

    @Override
    public boolean canSortBy(@NonNull String propertyName) {
        return subject.canSortBy(propertyName);
    }

    @Override
    public void add(I value) {
        subject.add(value);
    }

    @Override
    public void remove(I value) {
        subject.remove(value);
    }

    @Override
    public <T extends I> T update(T value, Patch patch) {
        return subject.update(value, patch);
    }

    @Override
    public void dispose() {
        subject.dispose();
    }

    @Override
    public Stream<I> findAll() {
        return subject.findAll();
    }

    @Override
    public <U extends I> Stream<U> findAll(Query<U> query) {
        return subject.findAll(query);
    }

    @Override
    public <U extends I> long count(final Class<U> of, final Filter filter) {
        return subject.count(of, filter);
    }

    @Override
    public <U extends I> Optional<U> findById(String id, Class<U> clazz) {
        return subject.findById(id, clazz);
    }

    @Override
    public <U extends I> Optional<U> findFirstByName(@NonNull String name, Class<U> clazz) {
        return subject.findFirstByName(name, clazz);
    }

    @Override
    public void syncTo(CatalogInfoRepository<I> target) {
        subject.syncTo(target);
    }
}
