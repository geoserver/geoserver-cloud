/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.feign;

import java.util.List;
import org.geoserver.catalog.StyleInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "catalog-service", contextId = "styleClient", path = "/api/v1/catalog/styles")
public interface StyleClient extends CatalogApiClient<StyleInfo> {

    @GetMapping(path = "/query/noworkspace", consumes = "application/stream+json")
    List<StyleInfo> findAllByNullWorkspace();

    @GetMapping(path = "/query/workspace", consumes = "application/stream+json")
    List<StyleInfo> findAllByWorkspaceId(@RequestParam(name = "workspaceId") String workspaceId);

    @GetMapping(path = "/find/name/{name}")
    StyleInfo findByNameAndWorkspaceId(
            @PathVariable("name") String name,
            @RequestParam(name = "workspaceId", required = false) String workspaceId);
}
