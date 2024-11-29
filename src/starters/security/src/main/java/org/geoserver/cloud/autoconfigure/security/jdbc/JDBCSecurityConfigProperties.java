/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.security.jdbc;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("geoserver.security")
@Data
public class JDBCSecurityConfigProperties {

    /** Enable or disable GeoServer JDBC Security plugin */
    private boolean jdbc = true;
}
