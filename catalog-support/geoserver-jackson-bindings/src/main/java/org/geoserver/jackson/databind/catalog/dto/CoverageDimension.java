package org.geoserver.jackson.databind.catalog.dto;

import java.util.List;
import lombok.Data;
import org.geoserver.catalog.impl.CoverageDimensionImpl;
import org.opengis.coverage.SampleDimensionType;

/** @see CoverageDimensionImpl */
public @Data class CoverageDimension {
    private String id;
    private String name;
    private String description;
    private NumberRangeDto range;
    private List<Double> nullValues;
    private String unit;
    private SampleDimensionType dimensionType;
}
