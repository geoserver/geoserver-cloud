package org.geoserver.cloud.catalog.dto;

import lombok.Data;
import org.geoserver.config.util.XStreamPersister;
import org.geotools.util.NumberRange;

/**
 * @see NumberRange
 * @see XStreamPersister#NumberRangeConverter
 */
public @Data class NumberRangeDto {

    private Double min;
    private Double max;
}
