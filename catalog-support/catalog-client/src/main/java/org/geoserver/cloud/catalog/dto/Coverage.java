package org.geoserver.cloud.catalog.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Coverage extends Resource {
    private String nativeFormat;
    private GridGeometryDto grid;
    private List<String> supportedFormats;
    private List<String> interpolationMethods;
    private String defaultInterpolationMethod;
    private List<CoverageDimension> dimensions;
    private List<String> requestSRS;
    private List<String> responseSRS;
    private Map<String, Serializable> parameters;
    private String nativeCoverageName;
}
