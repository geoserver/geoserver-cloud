/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.mapper;

import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.jackson.databind.catalog.dto.Namespace;
import org.mapstruct.Mapper;

@Mapper(config = CatalogInfoMapperConfig.class)
public interface NamespaceMapper {
    NamespaceInfo map(Namespace o);

    Namespace map(NamespaceInfo o);
}
