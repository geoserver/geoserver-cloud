/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.dto;

import java.math.BigDecimal;
import lombok.Data;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;

/** DTO for {@link DimensionInfo} */
@Data
public class Dimension {
    /**
     * @since geoserver 2.24.1
     */
    public enum NearestFailBehavior {
        IGNORE,
        EXCEPTION
    }

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

    /**
     * @since geoserver 2.23.0
     */
    private String startValue;

    /**
     * @since geoserver 2.23.0
     */
    private String endValue;

    /**
     * @since geoserver 2.24.1
     */
    private NearestFailBehavior nearestFailBehavior;
}
