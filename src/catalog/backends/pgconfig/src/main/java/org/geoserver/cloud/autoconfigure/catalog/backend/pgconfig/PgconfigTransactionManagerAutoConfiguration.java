/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@AutoConfiguration(after = PgconfigDataSourceAutoConfiguration.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnPgconfigBackendEnabled
@EnableTransactionManagement
public class PgconfigTransactionManagerAutoConfiguration {

    @Bean
    @DependsOnDatabaseInitialization
    DataSourceTransactionManager pgconfigTransactionManager(@Qualifier("pgconfigDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
