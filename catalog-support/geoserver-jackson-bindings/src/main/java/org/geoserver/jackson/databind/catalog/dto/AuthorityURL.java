package org.geoserver.jackson.databind.catalog.dto;

import lombok.Data;

public @Data class AuthorityURL {
    private String name;
    private String href;
}
