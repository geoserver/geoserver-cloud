package org.geoserver.cloud.catalog.dto;

import lombok.Data;

public @Data class DataLink {
    private String id;
    private String about;
    private String type;
    private String content;
}
