package org.geoserver.jackson.databind.catalog.dto;

import lombok.Data;

public @Data class LayerIdentifier {
    private String authority;
    private String identifier;
}
