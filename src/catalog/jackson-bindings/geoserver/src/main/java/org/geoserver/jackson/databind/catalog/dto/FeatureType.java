/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("FeatureTypeInfo")
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
    private String linearizationTolerance;
}
