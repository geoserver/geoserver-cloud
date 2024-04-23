/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.pgconfig;

import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * @since 1.4
 */
@Configuration
@EnableTransactionManagement
@EnableConfigurationProperties(PgsqlBackendProperties.class)
@Slf4j
public class PgsqlDataSourceConfiguration {

    @Bean
    @DependsOn("jndiInitializer")
    DataSource pgsqlConfigDatasource(PgsqlBackendProperties configprops) {
        DataSourceProperties config = configprops.getDatasource();
        String jndiName = config.getJndiName();
        if (StringUtils.hasText(jndiName)) {
            log.info("Creating pgsqlConfigDataSource from JNDI reference {}", jndiName);
            return new JndiDataSourceLookup().getDataSource(jndiName);
        }
        return config.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    PlatformTransactionManager pgconfigTransactionManager(
            @Qualifier("pgsqlConfigDatasource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "jndiInitializer")
    @ConditionalOnMissingBean(name = "jndiInitializer")
    Object jndiInitializerFallback() {
        log.warn(
                "jndiInitializer is not provided, beware a JNDI datasource definition for the pgsql catalog backend won't work.");
        return new Object();
    }
}
