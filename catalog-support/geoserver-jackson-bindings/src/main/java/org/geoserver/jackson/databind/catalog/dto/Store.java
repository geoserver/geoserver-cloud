package org.geoserver.jackson.databind.catalog.dto;

import java.io.Serializable;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class Store extends CatalogInfoDto {
    private String name;
    private InfoReference workspace;
    private String description;
    private String type;
    private boolean enabled;
    private Map<String, Serializable> connectionParameters;
    private Map<String, Serializable> metadata;
}
