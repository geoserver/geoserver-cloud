/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StoreClient {

    @GetMapping(path = "/stores/defaults")
    Flux<DataStoreInfo> getDefaultDataStores();

    @PutMapping(path = "/workspaces/{workspaceId}/stores/defaults/{dataStoreId}")
    void setDefaultDataStoreByWorkspaceId(//
            @NonNull @PathVariable("workspaceId") String workspaceId,
            @NonNull @RequestParam(name = "dataStoreId") String dataStoreId);

    @GetMapping(path = "/workspaces/{workspaceId}/stores/defaults")
    Mono<DataStoreInfo> findDefaultDataStoreByWorkspaceId(//
            @NonNull @PathVariable("workspaceId") String workspaceId);


    @GetMapping(path = "/workspaces/{workspaceId}/stores")
    Flux<StoreInfo> findStoresByWorkspaceId(//
            @NonNull @PathVariable("workspaceId") String workspaceId,
            @RequestParam(name = "type", required = false) ClassMappings subType);

    @GetMapping(path = "/workspaces/{workspaceId}/stores/{name}")
    Mono<StoreInfo> findStoreByWorkspaceIdAndName(//
            @NonNull @PathVariable("workspaceId") String workspaceId,
            @NonNull @RequestParam("name") String name,
            @RequestParam(name = "type", required = false) ClassMappings typeEnum);
}
