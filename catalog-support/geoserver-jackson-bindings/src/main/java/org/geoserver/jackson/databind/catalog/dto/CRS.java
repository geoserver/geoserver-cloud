package org.geoserver.jackson.databind.catalog.dto;

import lombok.Data;

public @Data class CRS {
    private String srs;
    private String WKT;
}
