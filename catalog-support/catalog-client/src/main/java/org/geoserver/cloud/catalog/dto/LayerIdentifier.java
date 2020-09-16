package org.geoserver.cloud.catalog.dto;

import lombok.Data;

public @Data class LayerIdentifier {
    private String authority;
    private String identifier;
}
