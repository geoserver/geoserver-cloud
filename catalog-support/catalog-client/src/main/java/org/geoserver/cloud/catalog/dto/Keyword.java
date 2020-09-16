package org.geoserver.cloud.catalog.dto;

import lombok.Data;

public @Data class Keyword {
    private String value;
    private String language;
    private String vocabulary;
}
