/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.reactivefeign;

import org.geoserver.catalog.LayerInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

public interface LayerClient {

    @GetMapping(path = "/layers/style/{styleId}")
    Flux<LayerInfo> findLayersWithStyle(@PathVariable("styleId") String styleId);

    @GetMapping(path = "/layers/resource/{resourceId}")
    Flux<LayerInfo> findLayersByResourceId(@RequestParam("resourceId") String resourceId);
}
