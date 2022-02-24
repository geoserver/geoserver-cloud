/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import lombok.Data;

import org.geoserver.catalog.impl.CoverageDimensionImpl;

import java.util.List;

/**
 * @see CoverageDimensionImpl
 */
public @Data class CoverageDimension {
    private String id;
    private String name;
    private String description;
    private NumberRangeDto range;
    private List<Double> nullValues;
    private String unit;
    private String dimensionType;
}
