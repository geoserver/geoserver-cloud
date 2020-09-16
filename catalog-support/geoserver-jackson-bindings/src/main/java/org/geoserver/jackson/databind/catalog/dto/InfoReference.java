package org.geoserver.jackson.databind.catalog.dto;

import lombok.Data;
import org.geoserver.catalog.impl.ClassMappings;

@Data
public class InfoReference {
    private ClassMappings type;
    private String id;
}
