/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import lombok.Getter;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.cloud.catalog.dto.Style;
import org.geoserver.cloud.catalog.modelmapper.StyleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(StyleController.BASE_URI)
public class StyleController extends AbstractCatalogInfoController<StyleInfo, Style> {

    public static final String BASE_URI = BASE_API_URI + "/styles";

    private final @Getter Class<StyleInfo> infoType = StyleInfo.class;

    private @Autowired StyleMapper mapper;

    protected @Override StyleInfo toInfo(Style dto) {
        return mapper.map(dto);
    }

    protected @Override Style toDto(StyleInfo info) {
        return mapper.map(info);
    }
}
