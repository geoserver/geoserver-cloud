package org.geoserver.jackson.databind.catalog.mapper;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.jackson.databind.catalog.dto.Workspace;
import org.mapstruct.Mapper;

@Mapper(config = SpringCatalogInfoMapperConfig.class)
public interface WorkspaceMapper {

    WorkspaceInfo map(Workspace o);

    Workspace map(WorkspaceInfo o);
}
