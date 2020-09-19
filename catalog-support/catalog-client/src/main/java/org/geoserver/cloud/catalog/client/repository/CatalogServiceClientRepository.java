/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import java.util.stream.Stream;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveCatalogClient;
import org.opengis.filter.Filter;
import org.springframework.lang.Nullable;
import lombok.NonNull;

public abstract class CatalogServiceClientRepository<CI extends CatalogInfo>
        implements CatalogInfoRepository<CI> {

    private ReactiveCatalogClient client;

    protected CatalogServiceClientRepository(@NonNull ReactiveCatalogClient client) {
        this.client = client;
    }

    protected abstract Class<CI> getInfoType();

    protected ReactiveCatalogClient client() {
        return client;
    }

    public @Override void add(CI value) {
        client.create(value);
    }

    public @Override void remove(CI value) {
        client.delete(value);
    }

    public @Override void update(CI value) {
        client.update(value);
    }

    public @Override void dispose() {
        // no-op...?
    }

    public @Override <U extends CI> U findFirstByName(@NonNull String name,
            @NonNull Class<U> infoType) {
        return infoType.cast(client.findByFirstByName(name, typeEnum(infoType)));
    }

    public @Override Stream<CI> findAll() {
        return client.findAll(typeEnum(getInfoType())).map(i -> (CI) i).toStream();
    }

    public @Override Stream<CI> findAll(Filter filter) {
        return client.query(typeEnum(getInfoType()), filter).map(i -> (CI) i).toStream();
    }

    public @Override <U extends CI> Stream<U> findAll(Filter filter, Class<U> infoType) {
        return client.query(typeEnum(infoType), filter).map(infoType::cast).toStream();
    }

    public @Override <U extends CI> U findById(@NonNull String id, @NonNull Class<U> clazz) {
        return client.findById(id, typeEnum(clazz)).map(clazz::cast).block();
    }

    public @Override void syncTo(CatalogInfoRepository<CI> target) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    protected @NonNull ClassMappings typeEnum(@NonNull Class<? extends Info> infoType) {
        ClassMappings enumVal = ClassMappings.fromInterface(infoType);
        if (enumVal == null) {
            enumVal = ClassMappings.fromImpl(infoType);
        }
        return enumVal;
    }
}
