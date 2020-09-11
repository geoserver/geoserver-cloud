/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.feign;

import org.geoserver.catalog.WorkspaceInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "catalog-service",
    url = "${geoserver.backend.catalog-service.uri:catalog-service}",
    contextId = "workspaceClient",
    path = "/api/v1/catalog/workspaces"
)
public interface WorkspaceClient extends CatalogApiClient<WorkspaceInfo> {

    @PostMapping(path = "/default", produces = XML)
    void setDefault(@RequestBody WorkspaceInfo workspace);

    @Nullable
    @GetMapping(path = "/default", consumes = XML)
    WorkspaceInfo getDefault();
}
