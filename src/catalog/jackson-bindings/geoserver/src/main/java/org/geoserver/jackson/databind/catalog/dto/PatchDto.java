/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Data;

import org.geotools.jackson.databind.filter.dto.Expression.Literal;

import java.util.Map;
import java.util.TreeMap;

/** DTO for {@link org.geoserver.catalog.plugin.Patch} */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("Patch")
public @Data class PatchDto {
    private Map<String, Literal> patches = new TreeMap<>();
}
