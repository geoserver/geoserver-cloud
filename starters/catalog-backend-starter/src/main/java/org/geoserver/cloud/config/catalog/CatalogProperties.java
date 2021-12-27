/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "geoserver.catalog")
public @Data class CatalogProperties {
    private boolean isolated = true;
    private boolean secure = true;
    private boolean localWorkspace = true;
    private boolean advertised = true;
}
