package org.geoserver.jackson.databind.catalog.dto;

import lombok.Data;
import org.geoserver.config.util.XStreamPersister;
import org.geotools.coverage.grid.GridGeometry2D;

/**
 * DTO for {@link GridGeometry2D}
 *
 * @see XStreamPersister#GridGeometry2DConverter
 */
public @Data class GridGeometryDto {

    // String.valueOf(g.getGridRange().getDimension())
    private String dimension;
    private String low;
    private String high;
    private double[] transform;
    private CRS crs;
}
