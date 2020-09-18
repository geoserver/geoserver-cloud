/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import org.geoserver.catalog.StyleInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StyleClient {

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
