/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveCatalogClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public abstract class CatalogServiceClientRepository<CI extends CatalogInfo>
        implements CatalogInfoRepository<CI> {

    private ReactiveCatalogClient client;

    /** */
    private Function<CI, CI> proxyResolver = c -> c;

    /** Don't use but through {@link #endpoint()} */
    private String _endpoint;

    protected ReactiveCatalogClient client() {
        return client;
    }

    protected String endpoint() {
        if (_endpoint == null) {
            this._endpoint =
                    ClassMappings.fromInterface(getContentType()).name().toLowerCase() + "s";
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
    protected <C extends CI> Optional<C> resolve(Optional<C> incoming) {
        return incoming.map(this::resolve);
    }

    protected <C extends CI> C resolve(C incoming) {
        Function<C, C> pr = proxyResolver();
        return pr.apply(incoming);
    }

    protected void block(Mono<Void> call) {
        if (Schedulers.isInNonBlockingThread()) {
            CompletableFuture.supplyAsync(call::block).join();
        }
        call.block();
    }

    protected <U extends CI> Optional<U> blockAndReturn(Mono<U> call) {
        if (Schedulers.isInNonBlockingThread()) {
            return CompletableFuture.supplyAsync(call::blockOptional).join();
        }
        return call.blockOptional();
    }

    private final ConcurrentMap<String, Boolean> positiveCanSortByCache = new ConcurrentHashMap<>();

    public @Override boolean canSortBy(@NonNull String propertyName) {
        Boolean canSort = positiveCanSortByCache.computeIfAbsent(propertyName, this::callCanSort);
        return canSort == null ? false : canSort.booleanValue();
    }

    private @Nullable Boolean callCanSort(String propertyName) {
        String endpoint = endpoint();
        Boolean canSort = client().canSortBy(endpoint, propertyName).toFuture().join();
        return canSort.booleanValue() ? Boolean.TRUE : null;
    }

    public @Override void add(@NonNull CI value) {
        blockAndReturn(client.create(endpoint(), value));
    }

    public @Override void remove(@NonNull CI value) {
        blockAndReturn(client.deleteById(endpoint(), value.getId()));
    }

    public @Override <T extends CI> T update(@NonNull T value, @NonNull Patch patch) {
        Mono<T> updated = client.update(endpoint(), value.getId(), patch);
        return blockAndReturn(updated).get();
    }

    public @Override <U extends CI> Optional<U> findFirstByName(
            @NonNull String name, @NonNull Class<U> infoType) {
        ClassMappings typeArg = typeEnum(infoType);
        Mono<U> found = client.findFirstByName(endpoint(), name, typeArg);
        return blockAndReturn(found);
    }

    public @Override Stream<CI> findAll() {
        ClassMappings typeArg = typeEnum(getContentType());
        Flux<CI> flux = client.findAll(endpoint(), typeArg);
        return flux.toStream();
    }

    public @Override <U extends CI> Stream<U> findAll(Query<U> query) {
        return client.query(endpoint(), query).map(this::resolve).toStream();
    }

    public @Override <U extends CI> Optional<U> findById(
            @NonNull String id, @NonNull Class<U> clazz) {
        ClassMappings typeArg = typeEnum(clazz);

        Optional<U> ret = blockAndReturn(client.findById(endpoint(), id, typeArg));
        return ret;
    }

    public @Override void dispose() {
        // no-op...?
    }

    public @Override void syncTo(@NonNull CatalogInfoRepository<CI> target) {
        findAll().forEach(target::add);
    }

    protected @NonNull ClassMappings typeEnum(@NonNull Class<? extends Info> infoType) {
        ClassMappings enumVal = ClassMappings.fromInterface(infoType);
        if (enumVal == null) {
            enumVal = ClassMappings.fromImpl(infoType);
        }
        return enumVal;
    }
}
