package org.geoserver.cloud.catalog.dto;

import java.io.Serializable;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Workspace extends CatalogInfoDto {
    private String name;
    private boolean isolated;
    private Map<String, Serializable> metadata;
}
