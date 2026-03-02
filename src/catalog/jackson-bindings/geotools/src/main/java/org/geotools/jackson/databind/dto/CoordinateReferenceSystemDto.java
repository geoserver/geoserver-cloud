/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

/**
 * DTO for {@link CoordinateReferenceSystem}
 */
@Data
public class CoordinateReferenceSystemDto {
    private String srs;

    @SuppressWarnings("java:S116")
    @JsonProperty("wkt")
    private String WKT;
}
