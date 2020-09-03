package org.geoserver.cloud.config.catalog;

import lombok.Data;

public @Data class CatalogProperties {
    private boolean secure = true;
    private boolean localWorkspace = true;
    private boolean advertised = true;
}
