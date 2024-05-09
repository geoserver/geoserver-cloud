/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.pgconfig;

import lombok.Data;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * @since 1.4
 */
@Data
@ConfigurationProperties("geoserver.backend.pgconfig")
public class PgconfigBackendProperties {

    private DataSourceProperties datasource;

    private String schema = "public";
    private boolean initialize = true;
    private boolean createSchema = true;

    public String schema() {
        return StringUtils.hasLength(schema) ? schema : "public";
    }
}
