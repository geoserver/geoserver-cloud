/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.feign;

import org.geoserver.catalog.StoreInfo;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "catalog-stores", path = "/api/v1/catalog/stores")
public interface StoreClient extends CatalogApiClient<StoreInfo> {}
