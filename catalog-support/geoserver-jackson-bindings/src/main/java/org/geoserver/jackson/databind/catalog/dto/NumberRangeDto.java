/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import lombok.Data;
import org.geoserver.config.util.XStreamPersister;
import org.geotools.util.NumberRange;

/**
 * @see NumberRange
 * @see XStreamPersister#NumberRangeConverter
 */
public @Data class NumberRangeDto {

    private Number min;
    private Number max;
    private boolean minIncluded;
    private boolean maxIncluded;
}
