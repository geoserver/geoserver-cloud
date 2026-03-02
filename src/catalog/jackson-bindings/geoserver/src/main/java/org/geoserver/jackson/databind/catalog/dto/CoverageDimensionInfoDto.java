/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Data;
import org.geotools.jackson.databind.dto.NumberRangeDto;

/**
 * DTO for CoverageDimensionInfo
 */
@Data
@JsonTypeName("CoverageDimensionInfo")
public class CoverageDimensionInfoDto {
    private String id;
    private String name;
    private String description;
    private NumberRangeDto range;
    private List<Double> nullValues;
    private String unit;
    private String dimensionType;
}
