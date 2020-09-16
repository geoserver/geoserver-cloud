package org.geoserver.cloud.catalog.dto;

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
