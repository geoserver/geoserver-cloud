/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.feign;

import java.util.List;
import org.geoserver.catalog.LayerInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "catalog-service", contextId = "layerClient", path = "/api/v1/catalog/layers")
public interface LayerClient extends CatalogApiClient<LayerInfo> {

    @GetMapping(path = "/query/styles", consumes = "application/stream+json")
    List<LayerInfo> findAllByDefaultStyleOrStyles(@RequestParam("styleId") String styleId);

    @GetMapping(path = "/query/resource", consumes = "application/stream+json")
    List<LayerInfo> findAllByResourceId(@RequestParam("resourceId") String resourceId);
}
