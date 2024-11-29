/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.pgconfig;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringUtils;

/**
 * @since 1.4
 */
@Configuration(proxyBeanMethods = true)
@EnableTransactionManagement
@EnableConfigurationProperties(PgconfigBackendProperties.class)
@Slf4j
public class PconfigDataSourceConfiguration {

    @Bean
    @DependsOn("jndiInitializer")
    DataSource pgconfigDataSource(PgconfigBackendProperties configprops) {
        DataSourceProperties config = configprops.getDatasource();
        String jndiName = config.getJndiName();
        if (StringUtils.hasText(jndiName)) {
            log.info("Creating pgconfigDataSource from JNDI reference {}", jndiName);
            return new JndiDataSourceLookup().getDataSource(jndiName);
        }
        return config.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(name = "jndiInitializer")
    @ConditionalOnMissingBean(name = "jndiInitializer")
    Object jndiInitializerFallback() {
        log.warn(
                """
                 jndiInitializer is not provided, beware a JNDI datasource \
                 definition for the pgconfig catalog backend won't work.
                 """);
        return new Object();
    }
}
