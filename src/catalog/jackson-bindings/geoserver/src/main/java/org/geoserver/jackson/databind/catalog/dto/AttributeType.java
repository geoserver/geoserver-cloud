/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import lombok.Data;
import lombok.Generated;

import java.io.Serializable;
import java.util.Map;

@Data
@Generated
public class AttributeType {
    private String name;
    private String featureType;

    private int minOccurs;
    private int maxOccurs;
    private boolean nillable;
    private Map<String, Serializable> metadata;
    private String binding;
    private Integer length;

    /**
     * Source expression (a valid CQL expression). If not set, it will default to the attribute name
     * (in AttributeTypeInfoImpl, not here, here it can be {@code null}).
     *
     * @since GeoServer 2.21
     */
    private String source;

    /**
     * @since geoserver 2.23.0
     */
    private Map<String, String> description;
}
