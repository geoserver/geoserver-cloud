package org.geoserver.jackson.databind.catalog.mapper;

import org.geoserver.catalog.StyleInfo;
import org.geoserver.jackson.databind.catalog.dto.Style;
import org.geotools.util.Version;
import org.mapstruct.Mapper;

@Mapper(config = SpringCatalogInfoMapperConfig.class)
public interface StyleMapper {

    StyleInfo map(Style o);

    Style map(StyleInfo o);

    default String map(org.geotools.util.Version version) {
        return version == null ? null : version.toString();
    }

    default org.geotools.util.Version map(String version) {
        return version == null ? null : new Version(version);
    }
}
