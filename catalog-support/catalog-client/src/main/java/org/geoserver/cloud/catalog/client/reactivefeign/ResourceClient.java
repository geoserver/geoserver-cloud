/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.NonNull;
import reactor.core.publisher.Mono;

public interface ResourceClient {

    @GetMapping(path = "/namespaces/{namespaceId}/resources/{name}")
    Mono<ResourceInfo> findResourceByNamespaceIdAndName(
            @NonNull @PathVariable("namespaceId") String namespaceId,
            @NonNull @PathVariable("name") String name,
            @RequestParam(name = "type", required = false) ClassMappings typeEnum);
}
