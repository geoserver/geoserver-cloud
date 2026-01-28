/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.config.catalog.backend.pgconfig;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @since 1.4
 */
@Configuration
@EnableConfigurationProperties(PgconfigBackendProperties.class)
public class DatabaseMigrationConfiguration {

    @Bean
    @DependsOnDatabaseInitialization
    Migrations pgconfigMigrations(
            PgconfigBackendProperties config, @Qualifier("pgconfigDataSource") DataSource dataSource) {

        return new Migrations(config, dataSource);
    }

    @RequiredArgsConstructor
    public static class Migrations implements InitializingBean {

        private final PgconfigBackendProperties config;
        private final DataSource dataSource;
        private PgconfigDatabaseMigrations databaseMigrations;

        @Override
        public void afterPropertiesSet() throws Exception {
            databaseMigrations = new PgconfigDatabaseMigrations()
                    .setInitialize(config.isInitialize())
                    .setDataSource(dataSource)
                    .setSchema(config.schema())
                    .setCreateSchema(config.isCreateSchema());
            databaseMigrations.migrate();
        }

        @Override
        public String toString() {
            PgconfigDatabaseMigrations m = databaseMigrations;
            return m == null ? "<migrations not yet run>" : m.toString();
        }
    }
}
