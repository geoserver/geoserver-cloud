/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.feign;

import java.util.List;
import org.geoserver.catalog.NamespaceInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "catalog-service",
    contextId = "namespaceClient",
    path = "/api/v1/catalog/namespaces"
)
public interface NamespaceClient extends CatalogApiClient<NamespaceInfo> {

    @PostMapping(path = "/default")
    void setDefault(@RequestBody NamespaceInfo namespace);

    @Nullable
    @GetMapping(path = "/default")
    NamespaceInfo getDefault();

    @Nullable
    @GetMapping(path = "/find/uri")
    NamespaceInfo findFirstByURI(@RequestParam(name = "uri") String uri);

    @GetMapping(path = "/query/uri", consumes = "application/stream+json")
    List<NamespaceInfo> findAllByURI(@RequestParam(name = "uri") String uri);
}
