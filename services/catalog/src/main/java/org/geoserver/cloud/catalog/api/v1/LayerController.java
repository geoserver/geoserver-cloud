/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import lombok.Getter;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.cloud.catalog.dto.Layer;
import org.geoserver.cloud.catalog.modelmapper.PublishedMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(LayerController.BASE_URI)
public class LayerController extends AbstractCatalogInfoController<LayerInfo, Layer> {

    public static final String BASE_URI = BASE_API_URI + "/layers";

    private final @Getter Class<LayerInfo> infoType = LayerInfo.class;

    private @Autowired PublishedMapper mapper;

    protected @Override LayerInfo toInfo(Layer dto) {
        return mapper.map(dto);
    }

    protected @Override Layer toDto(LayerInfo info) {
        return mapper.map(info);
    }
}
