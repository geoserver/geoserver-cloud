/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.repository;

import static org.geotools.filter.visitor.SimplifyingFilterVisitor.simplify;

import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.plugin.CatalogInfoRepository;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.cloud.catalog.client.reactivefeign.ReactiveCatalogClient;
import org.geoserver.cloud.catalog.client.repository.CatalogClientFilterSupport.PrePostFilterTuple;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.capability.FunctionName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public abstract class CatalogClientRepository<CI extends CatalogInfo>
        implements CatalogInfoRepository<CI> {

    private ReactiveCatalogClient client;

    private @Setter Function<CI, CI> objectResolver = Function.identity();
    private @Setter Supplier<Function<CatalogInfo, CatalogInfo>> streamResolver =
            () -> Function.identity();

    /** Don't use but through {@link #endpoint()} */
    private String _endpoint;

    /**
     * Splits {@link Query} filters into server-side supported and client-side post-processing
     * filters. Used and created lazily by {@link #findAll(Query)}
     */
    private CatalogClientFilterSupport filterSupport;

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

    @SuppressWarnings("unchecked")
    protected <C extends CI> Function<C, C> proxyResolver() {
        return (Function<C, C>) this.objectResolver;
    }

    /**
     * Resolves {@link ResolvingProxy resolving proxy} properties of {@code incoming}. The object
     * coming from the wire has all properties that are subtypes of {@link Info} "proxified" and
     * need to be resolved to the real ones before leaving this class
     */
    protected <C extends CI> C resolve(C incoming) {
        Function<C, C> pr = proxyResolver();
        C resolved = pr.apply(incoming);
        return resolved;
    }

    /**
     * Converts the Flux to a stream and applies {@link #proxyResolver()} function to each element
     * using a {@link MemoizingResolver} so the same reference is not requested multiple times to
     * the backend service while the stream is consumed
     */
    protected <I extends CI> Stream<I> toStream(Flux<I> flux) {
        @SuppressWarnings("unchecked")
        Function<I, I> resolver = (Function<I, I>) this.streamResolver.get();
        Stream<I> resolvingStream = flux.toStream().map(resolver::apply);
        return resolvingStream;
    }

    protected void block(Mono<Void> call) {
        if (Schedulers.isInNonBlockingThread()) {
            CompletableFuture.supplyAsync(call::block).join();
        }
        call.block();
    }

    protected <U> Optional<U> blockOptional(Mono<U> call) {
        Optional<U> object;
        if (Schedulers.isInNonBlockingThread()) {
            object = CompletableFuture.supplyAsync(call::blockOptional).join();
        } else {
            object = call.blockOptional();
        }
        return object;
    }

    protected <U extends CI> Optional<U> blockAndReturn(Mono<U> call) {
        Optional<U> object = blockOptional(call);
        return object.map(this::resolve);
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
        return toStream(flux);
    }

    public @Override <U extends CI> Stream<U> findAll(Query<U> query) {
        final Filter rawFilter = query.getFilter();
        if (Filter.EXCLUDE.equals(rawFilter)) {
            return Stream.empty(); // don't even bother
        }
        PrePostFilterTuple filters = getFilterSupport().split(rawFilter);
        Filter supportedFilter = simplify(filters.pre());
        Filter unsupportedFilter = simplify(filters.post());
        if (!Filter.INCLUDE.equals(supportedFilter)) {
            log.debug(
                    "Querying {}'s with filter {}",
                    query.getType().getSimpleName(),
                    supportedFilter);
        }
        query = query.withFilter(supportedFilter);
        return query(query, unsupportedFilter);
    }

    protected <U extends CI> Stream<U> query(Query<U> query, Filter unsupportedFilter) {
        Stream<U> stream = toStream(client.query(endpoint(), query));
        if (!Filter.INCLUDE.equals(unsupportedFilter)) {
            log.debug("Post-filtering with {}", unsupportedFilter);
            Predicate<? super U> predicate = info -> unsupportedFilter.evaluate(info);
            stream = stream.filter(predicate);
        }
        return stream;
    }

    public @Override <U extends CI> long count(@NonNull Class<U> of, @NonNull Filter rawFilter) {
        if (Filter.EXCLUDE.equals(rawFilter)) {
            return 0L;
        }
        PrePostFilterTuple filters = getFilterSupport().split(rawFilter);
        Filter supportedFilter = simplify(filters.pre());
        Filter unsupportedFilter = simplify(filters.post());
        Query<U> query = Query.valueOf(of, supportedFilter);
        if (Filter.INCLUDE.equals(unsupportedFilter)) {
            return blockOptional(client().count(endpoint(), query)).orElse(Long.valueOf(0));
        }
        return query(query, unsupportedFilter).count();
    }

    private CatalogClientFilterSupport getFilterSupport() {
        if (this.filterSupport == null) {
            List<FunctionName> serverFuncions = getServerSupportedFunctions();
            this.filterSupport = new CatalogClientFilterSupport(serverFuncions);
        }
        return this.filterSupport;
    }

    private List<FunctionName> getServerSupportedFunctions() {
        try {
            ReactiveCatalogClient client = client();
            Flux<FunctionName> functionNames = client.getSupportedFilterFunctionNames();
            List<FunctionName> list = functionNames.toStream().collect(Collectors.toList());
            return list;
        } catch (Exception e) {
            log.warn(
                    "Error getting server-side supported filter function names. Won't use functions.",
                    e);
            return Collections.emptyList();
        }
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
