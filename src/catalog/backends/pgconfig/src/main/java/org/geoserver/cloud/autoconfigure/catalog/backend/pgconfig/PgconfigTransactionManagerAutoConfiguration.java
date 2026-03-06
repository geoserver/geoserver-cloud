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

/**
 * Auto-configuration for the pgconfig backend's transaction manager.
 *
 * <p>Uses {@code @EnableTransactionManagement(proxyTargetClass = true)} to force CGLIB (class-based) proxies instead of
 * JDK dynamic proxies. This is required because
 * {@link org.geoserver.cloud.backend.pgconfig.resource.PgconfigResourceStore PgconfigResourceStore} is
 * {@code @Transactional} and implements {@link org.geoserver.platform.resource.ResourceStore ResourceStore}. With JDK
 * proxies (the default), Spring would create a proxy that only implements the {@code ResourceStore} interface, making
 * the bean unassignable to the concrete {@code PgconfigResourceStore} type needed by
 * {@link org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigGeoServerResourceLoader
 * PgconfigGeoServerResourceLoader} (which accesses {@code PgconfigResourceStore.getLockProvider()}, a method not on the
 * {@code ResourceStore} interface).
 *
 * <p>An alternative would be to extract {@code PgconfigResourceStore} as an interface exposing
 * {@code getLockProvider()}, but using CGLIB proxies is simpler and has no practical downsides.
 *
 * @since 1.4
 */
@AutoConfiguration(after = PgconfigDataSourceAutoConfiguration.class)
@SuppressWarnings("java:S1118") // Suppress SonarLint warning, constructor needs to be public
@ConditionalOnPgconfigBackendEnabled
@EnableTransactionManagement(proxyTargetClass = true)
public class PgconfigTransactionManagerAutoConfiguration {

    @Bean
    @DependsOnDatabaseInitialization
    DataSourceTransactionManager pgconfigTransactionManager(@Qualifier("pgconfigDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
