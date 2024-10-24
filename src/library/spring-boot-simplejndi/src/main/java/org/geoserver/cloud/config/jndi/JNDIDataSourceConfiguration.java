/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.jndi;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @since 1.0
 */
@Configuration
@EnableConfigurationProperties(JNDIDataSourcesConfigurationProperties.class)
public class JNDIDataSourceConfiguration {
    @Bean
    JNDIInitializer jndiInitializer(JNDIDataSourcesConfigurationProperties config) {
        return new JNDIInitializer(config);
    }
}
