package org.geoserver.cloud.catalog.dto;

import lombok.Data;

public @Data class Legend {
    private String id;
    private int width;
    private int height;
    private String format;
    private String onlineResource;
}
