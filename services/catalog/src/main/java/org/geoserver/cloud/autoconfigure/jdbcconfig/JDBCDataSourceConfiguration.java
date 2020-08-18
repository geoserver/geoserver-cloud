/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.jdbcconfig;

import javax.sql.DataSource;
import org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade;
import org.geoserver.jdbcstore.JDBCResourceStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configures the shared {@link DataSource} for {@link JDBCResourceStore} and {@link
 * JDBCCatalogFacade}
 */
@Configuration
@ConditionalOnProperty(prefix = "geoserver.jdbcconfig", name = "enabled", matchIfMissing = true)
@EnableTransactionManagement
public class JDBCDataSourceConfiguration {

    // <!-- data source, also loaded and configured via factory bean -->
    // <bean id="jdbcConfigDataSource"
    // class="org.geoserver.jdbcloader.DataSourceFactoryBean">
    // <constructor-arg ref="jdbcConfigProperties" />
    // </bean>
    @Bean(name = {"jdbcStoreDataSource", "jdbcConfigDataSource"})
    @ConfigurationProperties(prefix = "geoserver.jdbcconfig.datasource")
    public DataSource jdbcConfigDataSource() {
        return DataSourceBuilder.create().build();
    }

    // <!-- transaction manager -->
    // <bean id="jdbcConfigTransactionManager"
    // class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
    // <property name="dataSource" ref="jdbcConfigDataSource" />
    // </bean>
    // <tx:annotation-driven /> -> @EnableTransactionManagement above
    @Bean
    public DataSourceTransactionManager jdbcConfigTransactionManager() {
        return new DataSourceTransactionManager(jdbcConfigDataSource());
    }
}
