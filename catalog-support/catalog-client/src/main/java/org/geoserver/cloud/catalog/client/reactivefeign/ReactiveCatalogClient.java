/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

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
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.plugin.Patch;
import org.opengis.filter.Filter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ReactiveFeignClient( //
    name = "catalog-service", //
    url = "${geoserver.backend.catalog-service.uri:catalog-service}", //
    // contextId = "workspaceClient",//
    qualifier = "catalogClient", //
    path = "/api/v1/catalog"
)
public interface ReactiveCatalogClient {

    @PostMapping(path = "")
    Mono<CatalogInfo> create(CatalogInfo info);

    @PatchMapping(path = "/id/{id}")
    Mono<CatalogInfo> update(@PathVariable("id") String id, @NonNull @RequestBody Patch patch);

    @DeleteMapping(path = "")
    Mono<CatalogInfo> delete(CatalogInfo value);

    @GetMapping(path = "")
    Flux<CatalogInfo> findAll(@NonNull @RequestParam(name = "type") ClassMappings subType);

    @GetMapping(path = "/id/{id}")
    Mono<CatalogInfo> findById( //
            @NonNull @PathVariable("id") String id,
            @NonNull @RequestParam(name = "type") ClassMappings type);

    @GetMapping(path = "/name/{name}")
    Mono<CatalogInfo> findByFirstByName( //
            @NonNull @PathVariable(name = "name") String name,
            @NonNull @RequestParam(name = "type") ClassMappings subType);

    @PostMapping(path = "/query")
    Flux<CatalogInfo> query( //
            @NonNull @RequestParam(name = "type") ClassMappings subType,
            @NonNull @RequestBody Filter filter);

    @PutMapping(path = "/workspaces/default/{workspaceId}")
    Mono<WorkspaceInfo> setDefaultWorkspace(@PathVariable("workspaceId") String workspaceId);

    @GetMapping(path = "/workspaces/default")
    Mono<WorkspaceInfo> getDefaultWorkspace();

    @PostMapping(path = "namespaces/default/{namespaceId}")
    Mono<NamespaceInfo> setDefaultNamespace(
            @NonNull @PathVariable("namespaceId") String namespaceId);

    @GetMapping(path = "namespaces/default")
    Mono<NamespaceInfo> getDefaultNamespace();

    @GetMapping(path = "namespaces/uri")
    Mono<NamespaceInfo> findOneNamespaceByURI(@NonNull @RequestParam("uri") String uri);

    @GetMapping(path = "namespaces/uri/all")
    Flux<NamespaceInfo> findAllNamespacesByURI(@NonNull @RequestParam("uri") String uri);

    @GetMapping(path = "/stores/defaults")
    Flux<DataStoreInfo> getDefaultDataStores();

    @PutMapping(path = "/workspaces/{workspaceId}/stores/defaults/{dataStoreId}")
    Mono<DataStoreInfo> setDefaultDataStoreByWorkspaceId( //
            @NonNull @PathVariable("workspaceId") String workspaceId,
            @NonNull @RequestParam(name = "dataStoreId") String dataStoreId);

    @GetMapping(path = "/workspaces/{workspaceId}/stores/defaults")
    Mono<DataStoreInfo> findDefaultDataStoreByWorkspaceId( //
            @NonNull @PathVariable("workspaceId") String workspaceId);

    @GetMapping(path = "/workspaces/{workspaceId}/stores")
    Flux<StoreInfo> findStoresByWorkspaceId( //
            @NonNull @PathVariable("workspaceId") String workspaceId,
            @RequestParam(name = "type", required = false) ClassMappings subType);

    @GetMapping(path = "/workspaces/{workspaceId}/stores/{name}")
    Mono<StoreInfo> findStoreByWorkspaceIdAndName( //
            @NonNull @PathVariable("workspaceId") String workspaceId,
            @NonNull @RequestParam("name") String name,
            @RequestParam(name = "type", required = false) ClassMappings typeEnum);

    @GetMapping(path = "/namespaces/{namespaceId}/resources/{name}")
    Mono<ResourceInfo> findResourceByNamespaceIdAndName(
            @NonNull @PathVariable("namespaceId") String namespaceId,
            @NonNull @PathVariable("name") String name,
            @RequestParam(name = "type", required = false) ClassMappings typeEnum);

    @GetMapping(path = "/layers/style/{styleId}")
    Flux<LayerInfo> findLayersWithStyle(@PathVariable("styleId") String styleId);

    @GetMapping(path = "/layers/resource/{resourceId}")
    Flux<LayerInfo> findLayersByResourceId(@RequestParam("resourceId") String resourceId);

    @GetMapping(path = "/layergroups")
    Flux<LayerGroupInfo> findLayerGroupsByNullWoskspace();

    @GetMapping(path = "/workspaces/{workspaceId}/layergroups")
    Flux<LayerGroupInfo> findLayerGroupsByWoskspaceId(
            @PathVariable("workspaceId") String workspaceId);

    @GetMapping(path = "/layergroups/name/{name}")
    Mono<LayerGroupInfo> findLayerGroupByNameAndNullWorkspace(@PathVariable("name") String name);

    @GetMapping(path = "/workspaces/{workspaceId}/layergroups/{name}")
    Mono<LayerGroupInfo> findLayerGroupByNameAndWorkspaceId(
            @PathVariable(required = false, name = "workspaceId") String workspaceId,
            @PathVariable("name") String name);

    @GetMapping(path = "/styles")
    Flux<StyleInfo> findStylesByNullWorkspace();

    @GetMapping(path = "/workspaces/{workspaceId}/styles")
    Flux<StyleInfo> findStylesByWorkspaceId(
            @NonNull @PathVariable(name = "workspaceId") String workspaceId);

    @GetMapping(path = "/workspaces/{workspaceId}/styles/{name}")
    Mono<StyleInfo> findStyleByWorkspaceIdAndName(
            @NonNull @RequestParam(name = "workspaceId") String workspaceId,
            @NonNull @PathVariable("name") String name);

    @GetMapping(path = "/styles/{name}")
    Mono<StyleInfo> findStyleByNameAndNullWorkspace(@NonNull @PathVariable("name") String name);
}
