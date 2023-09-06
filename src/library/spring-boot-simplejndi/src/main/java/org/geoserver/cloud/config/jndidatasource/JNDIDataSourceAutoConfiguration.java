/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.jndidatasource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @since 1.0
 */
@AutoConfiguration
@EnableConfigurationProperties(JNDIDataSourcesConfigurationProperties.class)
public class JNDIDataSourceAutoConfiguration {
    @Bean
    JNDIInitializer jndiInitializer(JNDIDataSourcesConfigurationProperties config) {
        return new JNDIInitializer(config);
    }
}
