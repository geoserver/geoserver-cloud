package org.geoserver.jackson.databind.catalog.dto;

import lombok.Data;

public @Data class Attribution {
    private String id;
    private String title;
    private String href;
    private String logoURL;
    private int logoWidth;
    private int logoHeight;
    private String logoType;
}
