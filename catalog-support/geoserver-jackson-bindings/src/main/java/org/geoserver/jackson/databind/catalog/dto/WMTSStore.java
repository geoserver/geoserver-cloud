package org.geoserver.jackson.databind.catalog.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WMTSStore extends HTTPStore {
    private String headerName; // todo: replace with Map<String, String>
    private String headerValue; // todo: replace with Map<String, String>
}
