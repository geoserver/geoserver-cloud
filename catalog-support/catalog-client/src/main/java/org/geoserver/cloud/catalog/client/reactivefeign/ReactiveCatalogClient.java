/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

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
    qualifier = "catalogClient", //
    path = "/api/v1/catalog"
)
public interface ReactiveCatalogClient {

    @PostMapping(path = "/{endpoint}")
    <C extends CatalogInfo> Mono<C> create(@PathVariable("endpoint") String endpoint, C info);

    @PatchMapping(path = "/{endpoint}/{id}")
    public <C extends CatalogInfo> Mono<C> update(
            @PathVariable("endpoint") String endpoint,
            @PathVariable("id") String id,
            @RequestBody Patch patch);

    @DeleteMapping(path = "/{endpoint}/{id}")
    public <C extends CatalogInfo> Mono<C> deleteById(
            @PathVariable("endpoint") String endpoint, @PathVariable("id") String id);

    @GetMapping(path = "/{endpoint}")
    public Flux<CatalogInfo> findAll(
            @PathVariable("endpoint") String endpoint,
            @RequestParam(name = "type", required = false) ClassMappings subType);

    @GetMapping(path = {"/{endpoint}/{id}"})
    public Mono<CatalogInfo> findById( //
            @PathVariable("endpoint") String endpoint,
            @PathVariable("id") String id,
            @RequestParam(name = "type", required = false) ClassMappings subType);

    @GetMapping(path = "/{endpoint}/name/{name}/first")
    Mono<CatalogInfo> findFirstByName( //
            @PathVariable("endpoint") String endpoint,
            @PathVariable(name = "name") String name,
            @RequestParam(name = "type", required = false) ClassMappings subType);

    @PostMapping(path = "/{endpoint}/query")
    public Flux<CatalogInfo> query( //
            @PathVariable("endpoint") String endpoint,
            @RequestParam(name = "type", required = false) ClassMappings subType,
            @RequestBody Filter filter);

    @PutMapping(path = "/workspaces/default/{workspaceId}")
    Mono<WorkspaceInfo> setDefaultWorkspace(@PathVariable("workspaceId") String workspaceId);

    @GetMapping(path = "/workspaces/default")
    Mono<WorkspaceInfo> getDefaultWorkspace();

    @PutMapping(path = "namespaces/default/{namespaceId}")
    public Mono<NamespaceInfo> setDefaultNamespace(@PathVariable("namespaceId") String namespaceId);

    @GetMapping(path = "namespaces/default")
    Mono<NamespaceInfo> getDefaultNamespace();

    @GetMapping(path = "namespaces/uri")
    public Mono<NamespaceInfo> findOneNamespaceByURI(@RequestParam("uri") String uri);

    @GetMapping(path = "namespaces/uri/all")
    public Flux<NamespaceInfo> findAllNamespacesByURI(@RequestParam("uri") String uri);

    @GetMapping(path = "/stores/defaults")
    Flux<DataStoreInfo> getDefaultDataStores();

    @PutMapping(path = "/workspaces/{workspaceId}/stores/default/{dataStoreId}")
    Mono<DataStoreInfo> setDefaultDataStoreByWorkspaceId( //
            @PathVariable("workspaceId") String workspaceId,
            @PathVariable(name = "dataStoreId") String dataStoreId);

    @GetMapping(path = "/workspaces/{workspaceId}/stores/default")
    public Mono<DataStoreInfo> findDefaultDataStoreByWorkspaceId( //
            @PathVariable("workspaceId") String workspaceId);

    @GetMapping(path = "/workspaces/{workspaceId}/stores")
    public Flux<StoreInfo> findStoresByWorkspaceId( //
            @PathVariable("workspaceId") String workspaceId,
            @RequestParam(name = "type", required = false) ClassMappings subType);

    @GetMapping(path = "/workspaces/{workspaceId}/stores/name/{name}")
    public Mono<StoreInfo> findStoreByWorkspaceIdAndName( //
            @PathVariable("workspaceId") String workspaceId,
            @PathVariable("name") String name,
            @RequestParam(name = "type", required = false) ClassMappings subType);

    @GetMapping(path = "/namespaces/{namespaceId}/resources/name/{name}")
    <R extends ResourceInfo> Mono<R> findResourceByNamespaceIdAndName(
            @PathVariable("namespaceId") String namespaceId,
            @PathVariable("name") String name,
            @RequestParam(name = "type", required = false) ClassMappings typeEnum);

    @GetMapping(path = "/layers/style/{styleId}")
    Flux<LayerInfo> findLayersWithStyle(@PathVariable("styleId") String styleId);

    @GetMapping(path = "/layers/resource/{resourceId}")
    Flux<LayerInfo> findLayersByResourceId(@RequestParam("resourceId") String resourceId);

    @GetMapping(path = "/layergroups/noworkspace")
    Flux<LayerGroupInfo> findLayerGroupsByNullWoskspace();

    @GetMapping(path = "/workspaces/{workspaceId}/layergroups")
    Flux<LayerGroupInfo> findLayerGroupsByWoskspaceId(
            @PathVariable("workspaceId") String workspaceId);

    @GetMapping(path = "/layergroups/noworkspace/{name}")
    Mono<LayerGroupInfo> findLayerGroupByNameAndNullWorkspace(@PathVariable("name") String name);

    @GetMapping(path = "/workspaces/{workspaceId}/layergroups/{name}")
    Mono<LayerGroupInfo> findLayerGroupByWorkspaceIdAndName(
            @PathVariable(required = false, name = "workspaceId") String workspaceId,
            @PathVariable("name") String name);

    @GetMapping(path = "/styles/noworkspace")
    Flux<StyleInfo> findStylesByNullWorkspace();

    @GetMapping(path = "/workspaces/{workspaceId}/styles")
    Flux<StyleInfo> findStylesByWorkspaceId(@PathVariable(name = "workspaceId") String workspaceId);

    @GetMapping(path = "/workspaces/{workspaceId}/styles/{name}")
    public Mono<StyleInfo> findStyleByWorkspaceIdAndName(
            @PathVariable(name = "workspaceId") String workspaceId,
            @PathVariable("name") String name);

    @GetMapping(path = "/styles/noworkspace/{name}")
    Mono<StyleInfo> findStyleByNameAndNullWorkspace(@PathVariable("name") String name);
}
