/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.jackson.databind.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import org.geotools.geometry.jts.ReferencedEnvelope;

/** DTO for {@link ReferencedEnvelope} */
@Data
@JsonTypeName("ReferencedEnvelope")
public class ReferencedEnvelopeDto {
    private CoordinateReferenceSystemDto crs;
    private double[] coordinates;
}
