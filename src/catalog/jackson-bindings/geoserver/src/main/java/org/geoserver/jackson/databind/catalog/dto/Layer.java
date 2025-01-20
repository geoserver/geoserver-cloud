/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonTypeName("LayerInfo")
public class Layer extends Published {
    public enum WMSInterpolation {
        Nearest,
        Bilinear,
        Bicubic
    }

    protected String path;
    protected String defaultStyle;
    protected Set<String> styles;
    protected String resource;
    protected Legend legend;
    private PublishedType type;
    protected Boolean queryable;
    protected Boolean opaque;
    protected WMSInterpolation defaultWMSInterpolationMethod;
}
