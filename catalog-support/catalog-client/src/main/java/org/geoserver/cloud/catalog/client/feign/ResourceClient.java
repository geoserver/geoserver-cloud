/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.feign;

import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "catalog-service",
    contextId = "resourceClient",
    path = "/api/v1/catalog/resources"
)
public interface ResourceClient extends CatalogApiClient<ResourceInfo> {

    @GetMapping(path = "/find/name/{name}")
    ResourceInfo findByNameAndNamespaceId(
            @PathVariable("name") String name,
            @RequestParam(name = "namespaceId") String namespaceId,
            @RequestParam(name = "type", required = false) ClassMappings typeEnum);
}
