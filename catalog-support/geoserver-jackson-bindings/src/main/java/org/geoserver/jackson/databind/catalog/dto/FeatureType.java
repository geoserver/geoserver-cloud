package org.geoserver.jackson.databind.catalog.dto;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FeatureType extends Resource {
    private String cqlFilter;

    private int maxFeatures;
    private int numDecimals;
    private boolean padWithZeros;
    private boolean forcedDecimal;

    private List<AttributeType> attributes;
    private List<String> responseSRS;

    private boolean overridingServiceSRS;
    private boolean skipNumberMatched;
    private boolean circularArcPresent;
    private boolean encodeMeasures;
    private MeasureDto linearizationTolerance;
}
