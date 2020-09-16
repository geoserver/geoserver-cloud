package org.geoserver.cloud.catalog.dto;

import lombok.Data;
import org.geoserver.catalog.impl.ClassMappings;

@Data
public class InfoReference {
    private ClassMappings type;
    private String id;
}
