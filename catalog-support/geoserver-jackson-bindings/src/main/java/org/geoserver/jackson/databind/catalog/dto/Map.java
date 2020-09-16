package org.geoserver.jackson.databind.catalog.dto;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Map extends CatalogInfoDto {

    private String name;
    private boolean enabled;
    private List<InfoReference> layers;
}
