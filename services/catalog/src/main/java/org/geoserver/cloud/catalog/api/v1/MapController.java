/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import lombok.Getter;
import org.geoserver.catalog.MapInfo;
import org.geoserver.cloud.catalog.dto.Map;
import org.geoserver.cloud.catalog.modelmapper.MapMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(MapController.BASE_URI)
public class MapController extends AbstractCatalogInfoController<MapInfo, Map> {

    public static final String BASE_URI = BASE_API_URI + "/maps";

    private final @Getter Class<MapInfo> infoType = MapInfo.class;

    private @Autowired MapMapper mapper;

    protected @Override MapInfo toInfo(Map dto) {
        return mapper.map(dto);
    }

    protected @Override Map toDto(MapInfo info) {
        return mapper.map(info);
    }
}
