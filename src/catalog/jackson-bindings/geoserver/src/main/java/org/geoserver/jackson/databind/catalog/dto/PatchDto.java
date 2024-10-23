/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.geotools.jackson.databind.filter.dto.Literal;

/** DTO for {@link org.geoserver.catalog.plugin.Patch} */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonTypeName("Patch")
@Data
public class PatchDto {
    private List<PatchPropertyDto> patches = new ArrayList<>();

    @JsonTypeName("Property")
    public static @Data class PatchPropertyDto {
        private String name;
        private Literal value;
    }
}
