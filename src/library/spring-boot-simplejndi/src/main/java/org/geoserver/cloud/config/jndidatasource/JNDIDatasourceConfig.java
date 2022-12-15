/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.jndidatasource;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

/**
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JNDIDatasourceConfig extends DataSourceProperties {
    boolean enabled = true;
    boolean waitForIt = true;
    int waitTimeout = 60;

    int minimumIdle = 2;
    int maximumPoolSize = 10;
    long connectionTimeout = 250; // ms
    long idleTimeout = 60_000; // ms
}
