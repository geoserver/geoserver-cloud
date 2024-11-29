/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.LinkedHashMap;
import org.geotools.jackson.databind.filter.dto.Literal;

/**
 * @since 1.0
 */
@JsonTypeName("MetadataMap")
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
public class MetadataMapDto extends LinkedHashMap<String, Literal> {

    private static final long serialVersionUID = 1L;
}
