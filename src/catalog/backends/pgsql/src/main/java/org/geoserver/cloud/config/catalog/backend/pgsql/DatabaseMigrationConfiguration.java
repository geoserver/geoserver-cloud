/*
 * (c) 2023 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.pgsql;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @since 1.4
 */
@Configuration
@EnableConfigurationProperties(PgsqlBackendProperties.class)
public class DatabaseMigrationConfiguration {

    @Bean
    Migrations pgsqlMigrations(
            PgsqlBackendProperties config,
            @Qualifier("pgsqlConfigDatasource") DataSource dataSource) {

        return new Migrations(config, dataSource);
    }

    @RequiredArgsConstructor
    static class Migrations implements InitializingBean {

        private final PgsqlBackendProperties config;
        private final DataSource dataSource;

        @Override
        public void afterPropertiesSet() throws Exception {
            new PgsqlDatabaseMigrations()
                    .setInitialize(config.isInitialize())
                    .setDataSource(dataSource)
                    .setSchema(config.schema())
                    .setCreateSchema(config.isCreateSchema())
                    .migrate();
        }
    }
}
