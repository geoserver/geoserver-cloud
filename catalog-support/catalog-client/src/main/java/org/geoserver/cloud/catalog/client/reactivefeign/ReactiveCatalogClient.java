/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.opengis.filter.Filter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.NonNull;
import reactivefeign.spring.config.ReactiveFeignClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ReactiveFeignClient(//
        name = "catalog-service", //
        url = "${geoserver.backend.catalog-service.uri:catalog-service}", //
        // contextId = "workspaceClient",//
        qualifier = "catalogClient", //
        path = "/api/v1/catalog")
public interface ReactiveCatalogClient extends WorkspaceClient, NamespaceClient, StoreClient,
        ResourceClient, LayerClient, LayerGroupClient, StyleClient {

    @PostMapping(path = "")
    Mono<CatalogInfo> create(CatalogInfo info);

    @PutMapping(path = "")
    Mono<CatalogInfo> update(CatalogInfo info);

    @DeleteMapping(path = "")
    Mono<CatalogInfo> delete(CatalogInfo value);

    @GetMapping(path = "")
    Flux<CatalogInfo> findAll(
            @NonNull @RequestParam(name = "type") ClassMappings subType);

    @GetMapping(path = "/id/{id}")
    Mono<CatalogInfo> findById(//
            @NonNull @PathVariable("id") String id,
            @NonNull @RequestParam(name = "type") ClassMappings type);

    @GetMapping(path = "/name/{name}")
    Mono<CatalogInfo> findByFirstByName(//
            @NonNull @PathVariable(name = "name") String name,
            @NonNull @RequestParam(name = "type") ClassMappings subType);

    @PostMapping(path = "/query")
    Flux<CatalogInfo> query(//
            @NonNull @RequestParam(name = "type") ClassMappings subType,
            @NonNull @RequestBody Filter filter);

}

