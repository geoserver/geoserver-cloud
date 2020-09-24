/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.mapper;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.jackson.databind.catalog.dto.Workspace;
import org.mapstruct.Mapper;

@Mapper(config = CatalogInfoMapperConfig.class)
public interface WorkspaceMapper {

    WorkspaceInfo map(Workspace o);

    Workspace map(WorkspaceInfo o);
}
