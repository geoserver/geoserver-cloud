/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.feign;

import java.util.List;
import org.geoserver.catalog.LayerGroupInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "catalog-service",
    contextId = "layerGroupClient",
    path = "/api/v1/catalog/layergroups"
)
public interface LayerGroupClient extends CatalogApiClient<LayerGroupInfo> {

    @GetMapping(path = "/query/noworkspace")
    List<LayerGroupInfo> findAllByWoskspaceIsNull();

    @GetMapping(path = "/query/workspace")
    List<LayerGroupInfo> findAllByWoskspaceId(@RequestParam("workspaceId") String workspaceId);

    @GetMapping(path = "/find/name/{name}")
    LayerGroupInfo findByNameAndWorkspaceId(
            @PathVariable("name") String name,
            @RequestParam(required = false, name = "workspaceId") String workspaceId);
}
