/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Data;
import lombok.Generated;

import org.geotools.jackson.databind.filter.dto.Expression;

import java.util.ArrayList;
import java.util.List;

/** DTO for {@link org.geoserver.catalog.plugin.Patch} */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("Patch")
public @Data @Generated class PatchDto {
    private List<PatchPropertyDto> patches = new ArrayList<>();

    @JsonTypeName("Property")
    public static @Data class PatchPropertyDto {
        private String name;
        private Expression.Literal value;
    }
}
