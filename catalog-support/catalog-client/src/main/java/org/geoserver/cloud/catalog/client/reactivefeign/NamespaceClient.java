/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import lombok.NonNull;
import org.geoserver.catalog.NamespaceInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NamespaceClient {

    @PostMapping(path = "namespaces/default/{namespaceId}")
    void setDefaultNamespace(@NonNull @PathVariable("namespaceId") String namespaceId);

    @GetMapping(path = "namespaces/default")
    Mono<NamespaceInfo> getDefaultNamespace();

    @GetMapping(path = "namespaces/uri")
    Mono<NamespaceInfo> findOneNamespaceByURI(@NonNull @RequestParam("uri") String uri);

    @GetMapping(path = "namespaces/uri/all")
    Flux<NamespaceInfo> findAllNamespacesByURI(@NonNull @RequestParam("uri") String uri);
}
