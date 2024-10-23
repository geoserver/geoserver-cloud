/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.jndidatasource;

import java.util.Map;
import java.util.TreeMap;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @since 1.0
 */
@Data
@ConfigurationProperties(value = "jndi")
public class JNDIDataSourcesConfigurationProperties {

    private Map<String, JNDIDatasourceConfig> datasources = new TreeMap<>();
}
