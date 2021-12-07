/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

public @Data class CatalogProperties {
    @Value("${geoserver.catalog.isolated:true}")
    private boolean isolated = true;

    @Value("${geoserver.catalog.secure:true}")
    private boolean secure = true;

    @Value("${geoserver.catalog.local-workspace:true}")
    private boolean localWorkspace = true;

    @Value("${geoserver.catalog.advertised:true}")
    private boolean advertised = true;
}
