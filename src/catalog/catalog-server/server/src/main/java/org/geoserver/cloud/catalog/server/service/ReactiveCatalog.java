/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.server.service;

import lombok.NonNull;

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
import org.opengis.filter.Filter;
import org.opengis.filter.capability.FunctionName;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveCatalog {

    <C extends CatalogInfo> Mono<C> create(@NonNull Mono<C> info);

    <C extends CatalogInfo> Mono<C> update(@NonNull C info, @NonNull Mono<Patch> patch);

    <C extends CatalogInfo> Mono<C> delete(@NonNull C value);

    <C extends CatalogInfo> Flux<C> getAll(@NonNull Class<C> type);

    <C extends CatalogInfo> Mono<C> getById(@NonNull String id, @NonNull Class<C> type);

    <C extends CatalogInfo> Mono<C> getFirstByName(@NonNull String name, @NonNull Class<C> type);

    Mono<Boolean> canSortBy(Class<? extends CatalogInfo> type, String propertyName);

    <C extends CatalogInfo> Flux<C> query(@NonNull Query<C> query);

    <C extends CatalogInfo> Mono<Long> count(@NonNull Class<C> type, @NonNull Filter filter);

    Mono<WorkspaceInfo> setDefaultWorkspace(@NonNull WorkspaceInfo workspace);

    Mono<WorkspaceInfo> unsetDefaultWorkspace();

    Mono<WorkspaceInfo> getDefaultWorkspace();

    Mono<NamespaceInfo> setDefaultNamespace(@NonNull NamespaceInfo namespace);

    Mono<NamespaceInfo> unsetDefaultNamespace();

    Mono<NamespaceInfo> getDefaultNamespace();

    Mono<NamespaceInfo> getOneNamespaceByURI(@NonNull String uri);

    Flux<NamespaceInfo> getAllNamespacesByURI(@NonNull String uri);

    Flux<DataStoreInfo> getDefaultDataStores();

    Mono<DataStoreInfo> setDefaultDataStore(
            @NonNull WorkspaceInfo workspace, @NonNull DataStoreInfo dataStore);

    Mono<DataStoreInfo> unsetDefaultDataStore(@NonNull WorkspaceInfo workspace);

    Mono<DataStoreInfo> getDefaultDataStore(@NonNull WorkspaceInfo workspace);

    <S extends StoreInfo> Flux<S> getStoresByWorkspace(
            @NonNull WorkspaceInfo workspace, @NonNull Class<S> type);

    <S extends StoreInfo> Mono<S> getStoreByName(
            @NonNull WorkspaceInfo workspace, @NonNull String name, Class<S> type);

    <R extends ResourceInfo> Mono<R> getResourceByName(
            @NonNull NamespaceInfo namespace, @NonNull String name, @NonNull Class<R> type);

    Flux<LayerInfo> getLayersWithStyle(@NonNull StyleInfo style);

    Flux<LayerInfo> getLayersByResource(@NonNull ResourceInfo resource);

    Flux<LayerGroupInfo> getLayerGroupsWithNoWoskspace();

    Flux<LayerGroupInfo> getLayerGroupsByWoskspace(@NonNull WorkspaceInfo workspace);

    Mono<LayerGroupInfo> getLayerGroupByName(@NonNull String name);

    Mono<LayerGroupInfo> getLayerGroupByName(
            @NonNull WorkspaceInfo workspace, @NonNull String name);

    Flux<StyleInfo> getStylesWithNoWorkspace();

    Flux<StyleInfo> getStylesByWorkspace(@NonNull WorkspaceInfo workspace);

    Mono<StyleInfo> getStyleByName(@NonNull WorkspaceInfo workspace, @NonNull String name);

    Mono<StyleInfo> getStyleByName(@NonNull String name);

    Flux<FunctionName> getSupportedFunctionNames();
}
