/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.feign;

import java.util.List;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.ClassMappings;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "catalog-service", contextId = "storeClient", path = "/api/v1/catalog/stores")
public interface StoreClient extends CatalogApiClient<StoreInfo> {

    @PostMapping(path = "/default", produces = XML)
    void setDefaultDataStoreByWorkspaceId(
            @RequestParam(name = "workspaceId") String workspaceId,
            @RequestParam(name = "dataStoreId") String dataStoreId);

    @Nullable
    @GetMapping(path = "/default/{workspace}", consumes = XML)
    DataStoreInfo findDefaultDataStoreByWorkspaceId(
            @PathVariable("workspace") String workspaceName);

    @GetMapping(path = "/query/defaults", consumes = XML)
    List<DataStoreInfo> getDefaultDataStores();

    @GetMapping(path = "/query/workspace/{workspaceId}", consumes = XML)
    List<StoreInfo> findAllByWorkspaceId(
            @PathVariable("workspaceId") String workspaceId,
            @RequestParam(name = "type", required = false) ClassMappings subType);

    @GetMapping(path = "/query/all", consumes = XML)
    List<StoreInfo> findAllByType(
            @RequestParam(name = "type", required = false) ClassMappings subType);

    @GetMapping(path = "/find/{name}", consumes = XML)
    StoreInfo findByNameAndWorkspaceId(
            @PathVariable("name") String name,
            @RequestParam("workspaceId") String workspaceId,
            @RequestParam(name = "type", required = false) ClassMappings typeEnum);
}
