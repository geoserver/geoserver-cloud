/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveCatalogClient;
import org.opengis.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

public abstract class CatalogServiceClientRepository<CI extends CatalogInfo>
        implements CatalogInfoRepository<CI> {

    private @Lazy @Autowired ReactiveCatalogClient client;

    protected abstract Class<CI> getInfoType();

    protected ReactiveCatalogClient client() {
        return client;
    }

    public @Override void add(@NonNull CI value) {
        client.create(value).block();
    }

    public @Override void remove(@NonNull CI value) {
        client.delete(value).block();
    }

    public @Override void update(@NonNull CI value) {
        client.update(value).block();
    }

    public @Override void dispose() {
        // no-op...?
    }

    public @Override <U extends CI> U findFirstByName(
            @NonNull String name, @NonNull Class<U> infoType) {
        ClassMappings typeArg = typeEnum(infoType);
        Class<U> type = typeArg.getInterface();
        return client.findByFirstByName(name, typeArg).map(type::cast).block();
    }

    public @Override Stream<CI> findAll() {
        ClassMappings typeArg = typeEnum(getInfoType());
        Class<CI> type = typeArg.getInterface();
        return client.findAll(typeArg).map(type::cast).toStream();
    }

    public @Override Stream<CI> findAll(Filter filter) {
        ClassMappings typeArg = typeEnum(getInfoType());
        Class<CI> type = typeArg.getInterface();
        return client.query(typeArg, filter).map(type::cast).toStream();
    }

    public @Override <U extends CI> Stream<U> findAll(
            @NonNull Filter filter, @NonNull Class<U> infoType) {
        ClassMappings typeArg = typeEnum(infoType);
        return client.query(typeArg, filter).map(infoType::cast).toStream();
    }

    public @Override <U extends CI> U findById(@NonNull String id, @NonNull Class<U> clazz) {
        ClassMappings typeArg = typeEnum(clazz);
        return client.findById(id, typeArg).map(clazz::cast).block();
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
