/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import lombok.Getter;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.cloud.catalog.dto.Store;
import org.geoserver.cloud.catalog.modelmapper.StoreMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(StoreController.BASE_URI)
public class StoreController extends AbstractCatalogInfoController<StoreInfo, Store> {

    public static final String BASE_URI = BASE_API_URI + "/stores";

    private final @Getter Class<StoreInfo> infoType = StoreInfo.class;

    private @Autowired StoreMapper mapper;

    protected @Override StoreInfo toInfo(Store dto) {
        return mapper.map(dto);
    }

    protected @Override Store toDto(StoreInfo info) {
        return mapper.map(info);
    }
}
