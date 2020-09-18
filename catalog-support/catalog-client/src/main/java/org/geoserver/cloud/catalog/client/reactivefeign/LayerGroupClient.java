/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import org.geoserver.catalog.LayerGroupInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LayerGroupClient {

    @GetMapping(path = "/layergropus")
    Flux<LayerGroupInfo> findLayerGroupsByNullWoskspace();

    @GetMapping(path = "/workspaces/{workspaceId}/layergropus")
    Flux<LayerGroupInfo> findLayerGroupsByWoskspaceId(
            @PathVariable("workspaceId") String workspaceId);

    @GetMapping(path = "/layergropus/name/{name}")
    Mono<LayerGroupInfo> findLayerGropuByNameAndNullWorkspace(@PathVariable("name") String name);

    @GetMapping(path = "/workspaces/{workspaceId}/layergropus/{name}")
    Mono<LayerGroupInfo> findLayerGropuByNameAndWorkspaceId(
            @PathVariable(required = false, name = "workspaceId") String workspaceId,
            @PathVariable("name") String name
            );
}
