/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import lombok.Data;

import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;

import java.math.BigDecimal;

/** DTO for {@link DimensionInfo} */
public @Data class Dimension {
    private boolean enabled;
    private String attribute;
    private String endAttribute;
    private DimensionPresentation presentation;
    private BigDecimal resolution;
    private String units;
    private String unitSymbol;
    private Boolean nearestMatchEnabled;
    private Boolean rawNearestMatchEnabled;
    private String acceptableInterval;

    // defaultValue.strategy
    private DimensionDefaultValueSetting.Strategy defaultValueStrategy;
    // defaultValue.referenceValue
    private String defaultValueReferenceValue;
}
