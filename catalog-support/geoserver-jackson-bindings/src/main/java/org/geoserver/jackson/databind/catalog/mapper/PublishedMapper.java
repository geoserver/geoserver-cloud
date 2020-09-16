package org.geoserver.jackson.databind.catalog.mapper;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.jackson.databind.catalog.dto.Layer;
import org.geoserver.jackson.databind.catalog.dto.LayerGroup;
import org.mapstruct.Mapper;

@Mapper(config = SpringCatalogInfoMapperConfig.class)
public interface PublishedMapper {

    LayerInfo map(Layer o);

    Layer map(LayerInfo o);

    LayerGroupInfo map(LayerGroup o);

    LayerGroup map(LayerGroupInfo o);
}
