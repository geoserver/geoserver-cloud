/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import org.geotools.filter.FunctionFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.capability.FunctionName;
import org.opengis.parameter.Parameter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/** */
@Service
@Slf4j
public class ReactiveCatalogImpl implements ReactiveCatalog {

    private Scheduler catalogScheduler;

    private BlockingCatalog blockingCatalog;

    /** @see #getSupportedFunctionNames() */
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

    public @Override <C extends CatalogInfo> Mono<C> create(@NonNull Mono<C> info) {
        return info.subscribeOn(catalogScheduler).map(blockingCatalog::add);
    }

    public <C extends CatalogInfo> Mono<C> update(@NonNull C info, @NonNull Mono<Patch> patch) {
        return patch.subscribeOn(catalogScheduler).map(p -> blockingCatalog.update(info, p));
    }

    public @Override <C extends CatalogInfo> Mono<C> delete(@NonNull C info) {
        return Mono.just(info).subscribeOn(catalogScheduler).map(blockingCatalog::delete);
    }

    public @Override <C extends CatalogInfo> Flux<C> getAll(@NonNull Class<C> type) {
        return query(Query.all(type));
    }

    public @Override <C extends CatalogInfo> Mono<C> getById(
            @NonNull String id, @NonNull Class<C> type) {

        return async(() -> blockingCatalog.get(id, type));
    }

    public @Override <C extends CatalogInfo> Mono<C> getFirstByName(
            @NonNull String name, @NonNull Class<C> type) {

        return async(() -> blockingCatalog.getByName(name, type));
    }

    public @Override Mono<Boolean> canSortBy(
            Class<? extends CatalogInfo> type, String propertyName) {
        return async(() -> blockingCatalog.getFacade().canSort(type, propertyName));
    }

    public @Override <C extends CatalogInfo> Flux<C> query(@NonNull Query<C> query) {
        return Flux.fromStream(() -> blockingCatalog.query(query)).subscribeOn(catalogScheduler);
    }

    public @Override Flux<FunctionName> getSupportedFunctionNames() {
        return Flux.fromStream(this::supportedFunctionNames).subscribeOn(catalogScheduler);
    }

    private Stream<FunctionName> supportedFunctionNames() {
        if (supportedFilterFunctionNames == null) {
            List<FunctionName> names =
                    new FunctionFinder(null)
                            .getAllFunctionDescriptions()
                            .stream()
                            .filter(this::supportsdArgumentTypes)
                            .sorted((f1, f2) -> f1.getName().compareTo(f2.getName()))
                            .collect(Collectors.toList());
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
                        CoordinateReferenceSystem.class)
                .stream()
                .anyMatch(c -> c.isAssignableFrom(type));
    }

    public @Override Mono<WorkspaceInfo> setDefaultWorkspace(@NonNull WorkspaceInfo workspace) {
        return async(() -> blockingCatalog.setDefaultWorkspace(workspace), workspace);
    }

    public @Override Mono<WorkspaceInfo> unsetDefaultWorkspace() {
        return getDefaultWorkspace().doOnSuccess(ns -> blockingCatalog.setDefaultWorkspace(null));
    }

    public @Override Mono<NamespaceInfo> unsetDefaultNamespace() {
        return getDefaultNamespace().doOnSuccess(ns -> blockingCatalog.setDefaultNamespace(null));
    }

    public @Override Mono<DataStoreInfo> unsetDefaultDataStore(@NonNull WorkspaceInfo workspace) {
        return getDefaultDataStore(workspace)
                .doOnSuccess(ds -> blockingCatalog.setDefaultDataStore(workspace, null));
    }

    public @Override Mono<WorkspaceInfo> getDefaultWorkspace() {
        return async(blockingCatalog::getDefaultWorkspace);
    }

    public @Override Mono<NamespaceInfo> setDefaultNamespace(@NonNull NamespaceInfo namespace) {
        return async(() -> blockingCatalog.setDefaultNamespace(namespace), namespace);
    }

    public @Override Mono<NamespaceInfo> getDefaultNamespace() {
        return async(blockingCatalog::getDefaultNamespace);
    }

    public @Override Mono<NamespaceInfo> getOneNamespaceByURI(@NonNull String uri) {
        return Mono.just(uri).subscribeOn(catalogScheduler).map(blockingCatalog::getNamespaceByURI);
    }

    public @Override Flux<NamespaceInfo> getAllNamespacesByURI(@NonNull String uri) {
        return Flux.just(uri)
                .subscribeOn(catalogScheduler)
                .map(blockingCatalog::getNamespacesByURI)
                .flatMap(Flux::fromStream);
    }

    public @Override Flux<DataStoreInfo> getDefaultDataStores() {
        return Flux.fromStream(blockingCatalog::getDefaultDataStores).subscribeOn(catalogScheduler);
    }

    public @Override Mono<DataStoreInfo> setDefaultDataStore(
            @NonNull WorkspaceInfo workspace, @NonNull DataStoreInfo dataStore) {
        return async(() -> blockingCatalog.setDefaultDataStore(workspace, dataStore), dataStore);
    }

    public @Override Mono<DataStoreInfo> getDefaultDataStore(@NonNull WorkspaceInfo workspace) {
        return async(() -> blockingCatalog.getDefaultDataStore(workspace));
    }

    public @Override <S extends StoreInfo> Flux<S> getStoresByWorkspace(
            @NonNull WorkspaceInfo workspace, @NonNull Class<S> type) {

        return Flux.fromStream(() -> blockingCatalog.getStoresByWorkspace(workspace, type).stream())
                .subscribeOn(catalogScheduler);
    }

    public @Override <S extends StoreInfo> Mono<S> getStoreByName(
            @NonNull WorkspaceInfo workspace, @NonNull String name, Class<S> type) {

        return async(() -> blockingCatalog.getStoreByName(workspace, name, type));
    }

    public @Override <R extends ResourceInfo> Mono<R> getResourceByName(
            @NonNull NamespaceInfo namespace, @NonNull String name, @NonNull Class<R> type) {

        return async(() -> blockingCatalog.getResourceByName(namespace, name, type));
    }

    public @Override Flux<LayerInfo> getLayersWithStyle(@NonNull StyleInfo style) {
        return Flux.fromStream(() -> blockingCatalog.getLayers(style).stream())
                .subscribeOn(catalogScheduler);
    }

    public @Override Flux<LayerInfo> getLayersByResource(@NonNull ResourceInfo resource) {
        return Flux.fromStream(() -> blockingCatalog.getLayers(resource).stream())
                .subscribeOn(catalogScheduler);
    }

    public @Override Flux<LayerGroupInfo> getLayerGroupsWithNoWoskspace() {
        return Flux.fromStream(
                        () ->
                                blockingCatalog
                                        .getLayerGroupsByWorkspace(CatalogFacade.NO_WORKSPACE)
                                        .stream())
                .subscribeOn(catalogScheduler);
    }

    public @Override Flux<LayerGroupInfo> getLayerGroupsByWoskspace(
            @NonNull WorkspaceInfo workspace) {
        return Flux.fromStream(() -> blockingCatalog.getLayerGroupsByWorkspace(workspace).stream())
                .subscribeOn(catalogScheduler);
    }

    public @Override Mono<LayerGroupInfo> getLayerGroupByName(@NonNull String name) {
        return Mono.just(name)
                .subscribeOn(catalogScheduler)
                .map(blockingCatalog::getLayerGroupByName);
    }

    public @Override Mono<LayerGroupInfo> getLayerGroupByName(
            @NonNull WorkspaceInfo workspace, @NonNull String name) {
        return async(() -> blockingCatalog.getLayerGroupByName(workspace, name));
    }

    public @Override Flux<StyleInfo> getStylesWithNoWorkspace() {
        return Flux.fromStream(
                        () ->
                                blockingCatalog
                                        .getStylesByWorkspace(CatalogFacade.NO_WORKSPACE)
                                        .stream())
                .subscribeOn(catalogScheduler);
    }

    public @Override Flux<StyleInfo> getStylesByWorkspace(@NonNull WorkspaceInfo workspace) {
        return Flux.just(workspace)
                .subscribeOn(catalogScheduler)
                .map(blockingCatalog::getStylesByWorkspace)
                .flatMap(Flux::fromIterable);
    }

    public @Override Mono<StyleInfo> getStyleByName(
            @NonNull WorkspaceInfo workspace, @NonNull String name) {
        return async(() -> blockingCatalog.getStyleByName(workspace, name));
    }

    public @Override Mono<StyleInfo> getStyleByName(@NonNull String name) {
        return async(() -> blockingCatalog.getStyleByName(CatalogFacade.NO_WORKSPACE, name));
    }
}
