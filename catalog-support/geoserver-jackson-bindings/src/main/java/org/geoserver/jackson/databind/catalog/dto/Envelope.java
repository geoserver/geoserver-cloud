package org.geoserver.jackson.databind.catalog.dto;

import lombok.Data;

public @Data class Envelope {
    private CRS crs;
    private double[] coordinates;
}
