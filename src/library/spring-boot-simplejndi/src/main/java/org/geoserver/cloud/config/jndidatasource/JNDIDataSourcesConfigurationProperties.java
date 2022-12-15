/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.jndidatasource;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.TreeMap;

/**
 * @since 1.0
 */
@Data
@ConfigurationProperties(value = "jndi")
public class JNDIDataSourcesConfigurationProperties {

    private Map<String, JNDIDatasourceConfig> datasources = new TreeMap<>();
}
