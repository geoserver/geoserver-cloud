/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.mapper;

import lombok.Generated;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.jackson.databind.catalog.dto.StyleInfoDto;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;

@Mapper(config = CatalogInfoMapperConfig.class)
@AnnotateWith(value = Generated.class)
public interface StyleMapper {

    StyleInfo map(StyleInfoDto o);

    StyleInfoDto map(StyleInfo o);
}
