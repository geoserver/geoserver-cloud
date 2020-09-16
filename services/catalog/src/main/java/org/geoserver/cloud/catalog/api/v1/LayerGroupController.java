/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import lombok.Getter;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.cloud.catalog.dto.LayerGroup;
import org.geoserver.cloud.catalog.modelmapper.PublishedMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(LayerGroupController.BASE_URI)
public class LayerGroupController
        extends AbstractCatalogInfoController<LayerGroupInfo, LayerGroup> {

    public static final String BASE_URI = BASE_API_URI + "/layergroups";

    private final @Getter Class<LayerGroupInfo> infoType = LayerGroupInfo.class;

    private @Autowired PublishedMapper mapper;

    protected @Override LayerGroupInfo toInfo(LayerGroup dto) {
        return mapper.map(dto);
    }

    protected @Override LayerGroup toDto(LayerGroupInfo info) {
        return mapper.map(info);
    }
}
