/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.api.v1;

import lombok.Getter;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.cloud.catalog.dto.Namespace;
import org.geoserver.cloud.catalog.modelmapper.NamespaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(NamespaceController.BASE_URI)
public class NamespaceController extends AbstractCatalogInfoController<NamespaceInfo, Namespace> {

    public static final String BASE_URI = BASE_API_URI + "/namespaces";

    private final @Getter Class<NamespaceInfo> infoType = NamespaceInfo.class;

    private @Autowired NamespaceMapper mapper;

    protected @Override NamespaceInfo toInfo(Namespace dto) {
        return mapper.map(dto);
    }

    protected @Override Namespace toDto(NamespaceInfo info) {
        return mapper.map(info);
    }
}
