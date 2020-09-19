/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.catalog.plugin.forwarding;

import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.catalog.plugin.Patch;
import org.opengis.filter.Filter;

public abstract class ForwardingCatalogRepository<
                I extends CatalogInfo, S extends CatalogInfoRepository<I>>
        implements CatalogInfoRepository<I> {

    protected S subject;

    public ForwardingCatalogRepository(S subject) {
        this.subject = subject;
    }

    public @Override void add(I value) {
        subject.add(value);
    }

    public @Override void remove(I value) {
        subject.remove(value);
    }

    public @Override <T extends I> T update(T value, Patch patch) {
        return subject.update(value, patch);
    }

    public @Override void dispose() {
        subject.dispose();
    }

    public @Override Stream<I> findAll() {
        return subject.findAll();
    }

    public @Override Stream<I> findAll(Filter filter) {
        return subject.findAll(filter);
    }

    public @Override <U extends I> Stream<U> findAll(Filter filter, Class<U> infoType) {
        return subject.findAll(filter, infoType);
    }

    public @Override <U extends I> U findById(String id, Class<U> clazz) {
        return subject.findById(id, clazz);
    }

    public @Override <U extends I> U findFirstByName(@NonNull String name, Class<U> clazz) {
        return subject.findFirstByName(name, clazz);
    }

    public @Override void syncTo(CatalogInfoRepository<I> target) {
        subject.syncTo(target);
    }
}
