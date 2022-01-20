/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog;

import lombok.Data;
import org.geoserver.cloud.catalog.client.impl.CatalogClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "geoserver.backend")
public @Data class GeoServerBackendProperties {

    private DataDirectoryProperties dataDirectory = new DataDirectoryProperties();

    private JdbcconfigProperties jdbcconfig = new JdbcconfigProperties();

    private CatalogClientProperties catalogService = new CatalogClientProperties();
}
