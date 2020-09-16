/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.jackson.databind.catalog.dto;

import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Layer extends Published {
    public enum WMSInterpolation {
        Nearest,
        Bilinear,
        Bicubic
    }

    protected String path;
    protected InfoReference defaultStyle;
    protected Set<InfoReference> styles;
    protected InfoReference resource;
    protected Legend legend;
    private PublishedType type;
    protected Boolean queryable;
    protected Boolean opaque;
    protected WMSInterpolation defaultWMSInterpolationMethod;
}
