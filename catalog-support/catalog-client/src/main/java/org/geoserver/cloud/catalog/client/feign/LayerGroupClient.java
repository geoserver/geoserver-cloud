/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.feign;

import org.geoserver.catalog.LayerGroupInfo;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "catalog-layergroups", path = "/api/v1/catalog/layergroups")
public interface LayerGroupClient extends CatalogApiClient<LayerGroupInfo> {}
