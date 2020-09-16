package org.geoserver.jackson.databind.catalog.dto;

import java.io.Serializable;
import java.util.Map;
import lombok.Data;

@Data
public class AttributeType {
    private String name;
    private int minOccurs;
    private int maxOccurs;
    private boolean nillable;
    private Map<String, Serializable> metadata;
    private String binding;
    private Integer length;
}
