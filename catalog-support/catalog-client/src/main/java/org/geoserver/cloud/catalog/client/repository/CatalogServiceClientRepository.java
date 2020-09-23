/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveCatalogClient;
import org.opengis.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class CatalogServiceClientRepository<CI extends CatalogInfo>
        implements CatalogInfoRepository<CI> {

    private ReactiveCatalogClient client;

    /** */
    private Function<CI, CI> proxyResolver = c -> c;

    /** Don't use but through {@link #endpoint()} */
    private String _endpoint;

    protected abstract Class<CI> getInfoType();

    protected ReactiveCatalogClient client() {
        return client;
    }

    protected String endpoint() {
        if (_endpoint == null) {
            this._endpoint = ClassMappings.fromInterface(getInfoType()).name().toLowerCase() + "s";
        }
        return _endpoint;
    }

    @Autowired
    public void setClient(ReactiveCatalogClient client) {
        this.client = client;
    }

    public void setProxyResolver(Function<CI, CI> proxyResolver) {
        this.proxyResolver = proxyResolver;
    }

    @SuppressWarnings("unchecked")
    protected <C extends CI> Function<C, C> proxyResolver() {
        return (Function<C, C>) this.proxyResolver;
    }

    /**
     * Resolves {@link ResolvingProxy resolving proxy} properties of {@code incoming}. The object
     * coming from the wire has all properties that are subtypes of {@link Info} "proxified" and
     * need to be resolved to the real ones before leaving this class
     */
    protected <C extends CI> C resolve(C incoming) {
        Function<C, C> pr = proxyResolver();
        return pr.apply(incoming);
    }

    protected <C extends CI> Flux<C> resolve(Flux<? extends C> incoming) {
        Function<C, C> pr = proxyResolver();
        return incoming.map(pr::apply);
    }

    protected @Nullable <U extends CatalogInfo> U callAndBlock(Supplier<Mono<U>> callable) {
        Mono<U> call = callable.get();
        Mono<Optional<U>> mono = call.map(Optional::of).switchIfEmpty(Mono.just(Optional.empty()));
        // Stopwatch sw = Stopwatch.createStarted();
        U value = mono.block().orElse(null);
        // System.err.printf("blocked for %s, got %s %n", sw.stop(), value);
        return value;
    }

    protected @Nullable <U extends CI> U callAndReturn(Supplier<Mono<U>> callable) {
        return resolve(callAndBlock(callable));
    }

    public @Override void add(@NonNull CI value) {
        callAndBlock(() -> client.create(endpoint(), value));
    }

    public @Override void remove(@NonNull CI value) {
        callAndBlock(() -> client.deleteById(endpoint(), value.getId()));
    }

    @SuppressWarnings("unchecked")
    public @Override <T extends CI> T update(@NonNull T value, @NonNull Patch patch) {
        return callAndReturn(
                () ->
                        client.update(endpoint(), value.getId(), patch)
                                .map(r -> (T) r)
                                .map(this::resolve));
    }

    public @Override void dispose() {
        // no-op...?
    }

    public @Override <U extends CI> U findFirstByName(
            @NonNull String name, @NonNull Class<U> infoType) {
        ClassMappings typeArg = typeEnum(infoType);
        Class<U> type = typeArg.getInterface();

        return callAndReturn(
                () ->
                        client.findFirstByName(endpoint(), name, typeArg)
                                .map(type::cast)
                                .map(this::resolve));
    }

    public @Override Stream<CI> findAll() {
        ClassMappings typeArg = typeEnum(getInfoType());
        Class<CI> type = typeArg.getInterface();
        return client.findAll(endpoint(), typeArg).map(type::cast).map(this::resolve).toStream();
    }

    public @Override Stream<CI> findAll(Filter filter) {
        ClassMappings typeArg = typeEnum(getInfoType());
        Class<CI> type = typeArg.getInterface();
        return client.query(endpoint(), typeArg, filter)
                .map(type::cast)
                .map(this::resolve)
                .toStream();
    }

    public @Override <U extends CI> Stream<U> findAll(
            @NonNull Filter filter, @NonNull Class<U> infoType) {
        ClassMappings typeArg = typeEnum(infoType);
        return client.query(endpoint(), typeArg, filter)
                .map(infoType::cast)
                .map(this::resolve)
                .toStream();
    }

    public @Override <U extends CI> U findById(@NonNull String id, @NonNull Class<U> clazz) {
        ClassMappings typeArg = typeEnum(clazz);

        return callAndReturn(
                () -> client.findById(endpoint(), id, typeArg).map(clazz::cast).map(this::resolve));
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
