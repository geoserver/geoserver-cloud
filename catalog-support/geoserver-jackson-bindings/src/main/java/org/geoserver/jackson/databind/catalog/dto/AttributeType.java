/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
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
