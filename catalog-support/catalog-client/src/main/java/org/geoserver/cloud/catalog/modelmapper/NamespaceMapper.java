package org.geoserver.cloud.catalog.modelmapper;

import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.cloud.catalog.dto.Namespace;
import org.mapstruct.Mapper;

@Mapper(config = SpringCatalogInfoMapperConfig.class)
public interface NamespaceMapper {
    NamespaceInfo map(Namespace o);

    Namespace map(NamespaceInfo o);
}
