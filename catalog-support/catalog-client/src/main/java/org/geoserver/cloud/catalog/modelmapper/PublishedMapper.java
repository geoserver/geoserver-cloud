package org.geoserver.cloud.catalog.modelmapper;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.cloud.catalog.dto.Layer;
import org.geoserver.cloud.catalog.dto.LayerGroup;
import org.mapstruct.Mapper;

@Mapper(config = SpringCatalogInfoMapperConfig.class)
public interface PublishedMapper {

    LayerInfo map(Layer o);

    Layer map(LayerInfo o);

    LayerGroupInfo map(LayerGroup o);

    LayerGroup map(LayerGroupInfo o);
}
