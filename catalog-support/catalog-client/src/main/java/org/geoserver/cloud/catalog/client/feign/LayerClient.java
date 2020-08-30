/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.feign;

import org.geoserver.catalog.LayerInfo;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "catalog-layers", path = "/api/v1/catalog/layers")
public interface LayerClient extends CatalogApiClient<LayerInfo> {}
