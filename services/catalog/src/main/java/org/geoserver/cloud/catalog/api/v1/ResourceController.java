/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import lombok.Getter;
import org.geoserver.catalog.ResourceInfo;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ResourceController.BASE_URI)
public class ResourceController extends AbstractCatalogInfoController<ResourceInfo> {

    public static final String BASE_URI = BASE_API_URI + "/resources";

    private final @Getter Class<ResourceInfo> infoType = ResourceInfo.class;
}
