package org.geoserver.cloud.catalog.dto;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WMSLayer extends Resource {
    private String forcedRemoteStyle = "";
    private String preferredFormat;
    private Double minScale;
    private Double maxScale;
    private boolean metadataBBoxRespected;
    private List<String> selectedRemoteFormats;
    private List<String> selectedRemoteStyles;
}
