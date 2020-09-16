package org.geoserver.jackson.databind.catalog.dto;

import java.io.Serializable;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Namespace extends CatalogInfoDto {
    private String prefix;
    private String URI;
    private boolean isolated;
    private Map<String, Serializable> metadata;
}
