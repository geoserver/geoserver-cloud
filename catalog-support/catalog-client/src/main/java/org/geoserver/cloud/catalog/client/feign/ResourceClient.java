/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.feign;

import org.geoserver.catalog.ResourceInfo;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "catalog-resources", path = "/api/v1/catalog/resources")
public interface ResourceClient extends CatalogApiClient<ResourceInfo> {}
