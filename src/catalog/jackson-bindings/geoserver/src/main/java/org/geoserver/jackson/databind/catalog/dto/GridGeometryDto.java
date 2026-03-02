/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import org.geoserver.config.util.XStreamPersister;
import org.geotools.api.coverage.grid.GridGeometry;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.jackson.databind.dto.CoordinateReferenceSystemDto;

/**
 * DTO for {@link GridGeometry} (includes {@link GridGeometry2D})
 *
 * @see XStreamPersister#GridGeometry2DConverter
 */
@Data
@JsonTypeName("GridGeometry")
public class GridGeometryDto {
    private int[] low;
    private int[] high;
    private double[] transform;
    private CoordinateReferenceSystemDto crs;
}
