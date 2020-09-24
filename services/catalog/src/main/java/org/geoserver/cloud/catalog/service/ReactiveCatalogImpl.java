/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.service;

import java.util.concurrent.Callable;
import lombok.NonNull;
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
import org.opengis.filter.Filter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/** */
@Service
public class ReactiveCatalogImpl implements ReactiveCatalog {

    private Scheduler catalogScheduler;

    private BlockingCatalog blockingCatalog;

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
        return query(type, Filter.INCLUDE);
    }

    public @Override <C extends CatalogInfo> Mono<C> getById(
            @NonNull String id, @NonNull Class<C> type) {

        return async(() -> blockingCatalog.get(id, type));
    }

    public @Override <C extends CatalogInfo> Mono<C> getFirstByName(
            @NonNull String name, @NonNull Class<C> type) {

        return async(() -> blockingCatalog.getByName(name, type));
    }

    public @Override <C extends CatalogInfo> Flux<C> query(
            @NonNull Class<C> type, @NonNull Filter filter) {

        return Flux.fromStream(() -> blockingCatalog.query(type, filter))
                .subscribeOn(catalogScheduler);
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
