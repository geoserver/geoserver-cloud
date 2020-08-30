/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.jdbcconfig;

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
 * JDBCCatalogFacade} using {@code geoserver.jdbcconfig.datasource} as the {@code DataSource}
 * configuration key prefix.
 *
 * <p>See <a href=
 * "https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-configure-a-datasource">Configure
 * a Custom DataSource</a> in spring-boot's reference docs.
 *
 * <p>The following properties are the minimum necessary to configure the data source:
 *
 * <pre>{@code
 * geoserver.jdbcconfig.datasource.jdbc-url
 * geoserver.jdbcconfig.datasource.driver-class-name
 * geoserver.jdbcconfig.datasource.username
 * geoserver.jdbcconfig.datasource.password
 * geoserver.jdbcconfig.datasource.configuration
 * }</pre>
 *
 * Additionally, you can further configure the connection pool using any of the <a
 * href="https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby">Hikari</a> connection
 * pool property names, for example:
 *
 * <pre>{@code
 * geoserver.jdbcconfig.datasource.schema=public
 * geoserver.jdbcconfig.datasource.minimumIdle=2
 * geoserver.jdbcconfig.datasource.maximumPoolSize=10
 * }</pre>
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
