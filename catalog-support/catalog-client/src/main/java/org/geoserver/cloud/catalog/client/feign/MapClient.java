/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.feign;

import org.geoserver.catalog.MapInfo;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "catalog-maps", path = "/api/v1/catalog/maps")
public interface MapClient extends CatalogApiClient<MapInfo> {}
