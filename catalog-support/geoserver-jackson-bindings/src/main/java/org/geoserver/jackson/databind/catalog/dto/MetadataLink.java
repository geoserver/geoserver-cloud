package org.geoserver.jackson.databind.catalog.dto;

import lombok.Data;

public @Data class MetadataLink {
    private String id;
    private String type;
    private String about;
    private String metadataType;
    private String content;
}
