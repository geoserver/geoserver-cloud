package org.geoserver.cloud.catalog.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CoverageStore extends Store {

    private String URL;
}
