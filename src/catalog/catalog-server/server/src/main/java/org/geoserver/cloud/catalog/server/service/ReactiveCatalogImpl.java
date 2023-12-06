/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.server.service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.plugin.Patch;
import org.geoserver.catalog.plugin.Query;
import org.geoserver.function.IsInstanceOf;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.api.parameter.Parameter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.filter.FunctionFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** */
@Service
@Slf4j
public class ReactiveCatalogImpl implements ReactiveCatalog {

    private Scheduler catalogScheduler;

    private BlockingCatalog blockingCatalog;

    /**
     * @see #getSupportedFunctionNames()
     */
    private List<FunctionName> supportedFilterFunctionNames;

    public ReactiveCatalogImpl(
            BlockingCatalog blockingCatalog,
            @Qualifier("catalogScheduler") Scheduler catalogScheduler) {
        this.blockingCatalog = blockingCatalog;
        this.catalogScheduler = catalogScheduler;
    }

    private <T> Mono<T> async(Callable<T> callable) {
        return Mono.fromCallable(callable).subscribeOn(catalogScheduler);
    }

    private <T> Mono<T> async(Runnable runnable, T returnValue) {
        return Mono.fromRunnable(runnable).subscribeOn(catalogScheduler).thenReturn(returnValue);
    }

    @Override
    public <C extends CatalogInfo> Mono<C> create(@NonNull Mono<C> info) {
        return info.subscribeOn(catalogScheduler).map(blockingCatalog::add);
    }

    public <C extends CatalogInfo> Mono<C> update(@NonNull C info, @NonNull Mono<Patch> patch) {
        return patch.subscribeOn(catalogScheduler).map(p -> blockingCatalog.update(info, p));
    }

    @Override
    public <C extends CatalogInfo> Mono<C> delete(@NonNull C info) {
        return Mono.just(info).subscribeOn(catalogScheduler).map(blockingCatalog::delete);
    }

    @Override
    public <C extends CatalogInfo> Flux<C> getAll(@NonNull Class<C> type) {
        return query(Query.all(type));
    }

    @Override
    public <C extends CatalogInfo> Mono<C> getById(@NonNull String id, @NonNull Class<C> type) {

        return async(() -> blockingCatalog.get(id, type));
    }

    @Override
    public <C extends CatalogInfo> Mono<C> getFirstByName(
            @NonNull String name, @NonNull Class<C> type) {

        return async(() -> blockingCatalog.getByName(name, type));
    }

    @Override
    public Mono<Boolean> canSortBy(Class<? extends CatalogInfo> type, String propertyName) {
        return async(() -> blockingCatalog.getFacade().canSort(type, propertyName));
    }

    @Override
    public <C extends CatalogInfo> Flux<C> query(@NonNull Query<C> query) {
        log.debug(
                "Processing request query of {} with filter {}",
                query.getType().getSimpleName(),
                query.getFilter());
        return Flux.fromStream(() -> blockingCatalog.query(query)).subscribeOn(catalogScheduler);
    }

    @Override
    public <C extends CatalogInfo> Mono<Long> count(
            @NonNull Class<C> type, @NonNull Filter filter) {

        return async(() -> (long) blockingCatalog.count(type, filter));
    }

    @Override
    public Flux<FunctionName> getSupportedFunctionNames() {
        return Flux.fromStream(this::supportedFunctionNames).subscribeOn(catalogScheduler);
    }

    private Stream<FunctionName> supportedFunctionNames() {
        if (supportedFilterFunctionNames == null) {
            List<FunctionName> names =
                    new FunctionFinder(null)
                            .getAllFunctionDescriptions().stream()
                                    .filter(this::supportsdArgumentTypes)
                                    .sorted((f1, f2) -> f1.getName().compareTo(f2.getName()))
                                    .collect(Collectors.toCollection(LinkedList::new));
            if (!names.contains(IsInstanceOf.NAME)) {
                names.add(0, IsInstanceOf.NAME);
            }
            supportedFilterFunctionNames = names;
        }
        return supportedFilterFunctionNames.stream();
    }

    private boolean supportsdArgumentTypes(FunctionName fn) {
        try {
            List<Parameter<?>> args = fn.getArguments();
            for (Parameter<?> p : args) {
                Class<?> type = p.getType();
                if (!isCommonParamType(type)) {
                    log.debug(
                            "FunctionName {} not supported, parameter type {} is not considered safe",
                            fn.getName(),
                            type.getCanonicalName());
                    return false;
                }
            }
            return true;
        } catch (RuntimeException e) {
            log.warn(
                    "Error figuring out of function {} is supported: {}",
                    fn.getName(),
                    e.getMessage());
            return false;
        }
    }

    /** Does it look like something we could send/receive over the wire? */
    private boolean isCommonParamType(Class<?> type) {
        if (type.isPrimitive()) return true;
        return Arrays.asList(
                        Number.class,
                        Boolean.class,
                        CharSequence.class,
                        Date.class,
                        Geometry.class,
                        ReferencedEnvelope.class,
                        CoordinateReferenceSystem.class,
                        Class.class)
                .stream()
                .anyMatch(c -> c.isAssignableFrom(type));
    }

    @Override
    public Mono<WorkspaceInfo> setDefaultWorkspace(@NonNull WorkspaceInfo workspace) {
        return async(() -> blockingCatalog.setDefaultWorkspace(workspace), workspace);
    }

    @Override
    public Mono<WorkspaceInfo> unsetDefaultWorkspace() {
        return getDefaultWorkspace().doOnSuccess(ns -> blockingCatalog.setDefaultWorkspace(null));
    }

    @Override
    public Mono<NamespaceInfo> unsetDefaultNamespace() {
        return getDefaultNamespace().doOnSuccess(ns -> blockingCatalog.setDefaultNamespace(null));
    }

    @Override
    public Mono<DataStoreInfo> unsetDefaultDataStore(@NonNull WorkspaceInfo workspace) {
        return getDefaultDataStore(workspace)
                .doOnSuccess(ds -> blockingCatalog.setDefaultDataStore(workspace, null));
    }

    @Override
    public Mono<WorkspaceInfo> getDefaultWorkspace() {
        return async(blockingCatalog::getDefaultWorkspace);
    }

    @Override
    public Mono<NamespaceInfo> setDefaultNamespace(@NonNull NamespaceInfo namespace) {
        return async(() -> blockingCatalog.setDefaultNamespace(namespace), namespace);
    }

    @Override
    public Mono<NamespaceInfo> getDefaultNamespace() {
        return async(blockingCatalog::getDefaultNamespace);
    }

    @Override
    public Mono<NamespaceInfo> getOneNamespaceByURI(@NonNull String uri) {
        return Mono.just(uri).subscribeOn(catalogScheduler).map(blockingCatalog::getNamespaceByURI);
    }

    @Override
    public Flux<NamespaceInfo> getAllNamespacesByURI(@NonNull String uri) {
        return Flux.just(uri)
                .subscribeOn(catalogScheduler)
                .map(blockingCatalog::getNamespacesByURI)
                .flatMap(Flux::fromStream);
    }

    @Override
    public Flux<DataStoreInfo> getDefaultDataStores() {
        return Flux.fromStream(blockingCatalog::getDefaultDataStores).subscribeOn(catalogScheduler);
    }

    @Override
    public Mono<DataStoreInfo> setDefaultDataStore(
            @NonNull WorkspaceInfo workspace, @NonNull DataStoreInfo dataStore) {
        return async(() -> blockingCatalog.setDefaultDataStore(workspace, dataStore), dataStore);
    }

    @Override
    public Mono<DataStoreInfo> getDefaultDataStore(@NonNull WorkspaceInfo workspace) {
        return async(() -> blockingCatalog.getDefaultDataStore(workspace));
    }

    @Override
    public <S extends StoreInfo> Flux<S> getStoresByWorkspace(
            @NonNull WorkspaceInfo workspace, @NonNull Class<S> type) {

        return Flux.fromStream(() -> blockingCatalog.getStoresByWorkspace(workspace, type).stream())
                .subscribeOn(catalogScheduler);
    }

    @Override
    public <S extends StoreInfo> Mono<S> getStoreByName(
            @NonNull WorkspaceInfo workspace, @NonNull String name, Class<S> type) {

        return async(() -> blockingCatalog.getStoreByName(workspace, name, type));
    }

    @Override
    public <R extends ResourceInfo> Mono<R> getResourceByName(
            @NonNull NamespaceInfo namespace, @NonNull String name, @NonNull Class<R> type) {

        return async(() -> blockingCatalog.getResourceByName(namespace, name, type));
    }

    @Override
    public Flux<LayerInfo> getLayersWithStyle(@NonNull StyleInfo style) {
        return Flux.fromStream(() -> blockingCatalog.getLayers(style).stream())
                .subscribeOn(catalogScheduler);
    }

    @Override
    public Flux<LayerInfo> getLayersByResource(@NonNull ResourceInfo resource) {
        return Flux.fromStream(() -> blockingCatalog.getLayers(resource).stream())
                .subscribeOn(catalogScheduler);
    }

    @Override
    public Flux<LayerGroupInfo> getLayerGroupsWithNoWoskspace() {
        return Flux.fromStream(
                        () ->
                                blockingCatalog
                                        .getLayerGroupsByWorkspace(CatalogFacade.NO_WORKSPACE)
                                        .stream())
                .subscribeOn(catalogScheduler);
    }

    @Override
    public Flux<LayerGroupInfo> getLayerGroupsByWoskspace(@NonNull WorkspaceInfo workspace) {
        return Flux.fromStream(() -> blockingCatalog.getLayerGroupsByWorkspace(workspace).stream())
                .subscribeOn(catalogScheduler);
    }

    @Override
    public Mono<LayerGroupInfo> getLayerGroupByName(@NonNull String name) {
        return Mono.just(name)
                .subscribeOn(catalogScheduler)
                .map(blockingCatalog::getLayerGroupByName);
    }

    @Override
    public Mono<LayerGroupInfo> getLayerGroupByName(
            @NonNull WorkspaceInfo workspace, @NonNull String name) {
        return async(() -> blockingCatalog.getLayerGroupByName(workspace, name));
    }

    @Override
    public Flux<StyleInfo> getStylesWithNoWorkspace() {
        return Flux.fromStream(
                        () ->
                                blockingCatalog
                                        .getStylesByWorkspace(CatalogFacade.NO_WORKSPACE)
                                        .stream())
                .subscribeOn(catalogScheduler);
    }

    @Override
    public Flux<StyleInfo> getStylesByWorkspace(@NonNull WorkspaceInfo workspace) {
        return Flux.just(workspace)
                .subscribeOn(catalogScheduler)
                .map(blockingCatalog::getStylesByWorkspace)
                .flatMap(Flux::fromIterable);
    }

    @Override
    public Mono<StyleInfo> getStyleByName(@NonNull WorkspaceInfo workspace, @NonNull String name) {
        return async(() -> blockingCatalog.getStyleByName(workspace, name));
    }

    @Override
    public Mono<StyleInfo> getStyleByName(@NonNull String name) {
        return async(() -> blockingCatalog.getStyleByName(CatalogFacade.NO_WORKSPACE, name));
    }
}
