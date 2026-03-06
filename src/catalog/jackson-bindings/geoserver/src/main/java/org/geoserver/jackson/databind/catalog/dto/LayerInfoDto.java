/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** DTO for {@link org.geoserver.catalog.LayerInfo} */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("LayerInfo")
public class LayerInfoDto extends PublishedInfoDto {
    /** DTO for {@link org.geoserver.catalog.LayerInfo.WMSInterpolation} */
    @JsonTypeName("WMSInterpolation")
    @SuppressWarnings("java:S115")
    public enum WMSInterpolationDto {
        Nearest,
        Bilinear,
        Bicubic
    }

    protected String path;
    protected String defaultStyle;
    protected Set<String> styles;
    protected String resource;
    protected LegendInfoDto legend;
    private PublishedTypeDto type;
    protected Boolean queryable;
    protected Boolean opaque;
    protected WMSInterpolationDto defaultWMSInterpolationMethod;
}
