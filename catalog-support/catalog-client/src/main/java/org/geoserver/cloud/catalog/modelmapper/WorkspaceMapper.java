package org.geoserver.cloud.catalog.modelmapper;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.cloud.catalog.dto.Workspace;
import org.mapstruct.Mapper;

@Mapper(config = SpringCatalogInfoMapperConfig.class)
public interface WorkspaceMapper {

    WorkspaceInfo map(Workspace o);

    Workspace map(WorkspaceInfo o);
}
