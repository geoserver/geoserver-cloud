/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.backend;

import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.ConditionalOnPgconfigBackendEnabled;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.PgconfigDataSourceAutoConfiguration;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.PgconfigMigrationAutoConfiguration;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnDiskQuotaEnabled;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigBackendProperties;
import org.geoserver.gwc.config.GeoserverXMLResourceProvider;
import org.geowebcache.diskquota.ConfigLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Pre-configures GeoWebCache's DiskQuota subsystem to use the same database as the pgconfig
 * catalog backend.
 *
 * <p>On first start (when neither {@code geowebcache-diskquota.xml} nor
 * {@code geowebcache-diskquota-jdbc.xml} exists in the GWC config dir) writes default copies that:
 *
 * <ul>
 *   <li>select the upstream JDBC quota store ({@code <quotaStore>JDBC</quotaStore>}), and
 *   <li>point it at the pgconfig JNDI DataSource and schema, with the {@code PostgreSQL} dialect.
 * </ul>
 *
 * <p>After that point the standard upstream flow drives store creation:
 * {@code DiskQuotaMonitor} -> {@code ConfigurableQuotaStoreProvider#reloadQuotaStore} ->
 * {@code JDBCQuotaStoreFactory#getJDBCStore} -> JNDI lookup -> {@code JDBCQuotaStore}, with
 * {@link org.geowebcache.diskquota.jdbc.JDBCConfiguration#getSchema() the schema} honored at
 * {@code JDBCQuotaStoreFactory:174}. The user can later edit the configuration through the
 * {@code DiskQuotaSettingsPage} UI; we never overwrite an existing file.
 *
 * <p>Activates only when:
 *
 * <ul>
 *   <li>{@link ConditionalOnDiskQuotaEnabled gwc.disk-quota.enabled=true},
 *   <li>{@link ConditionalOnPgconfigBackendEnabled geoserver.backend.pgconfig.enabled=true}, and
 *   <li>pgconfig is itself JNDI-backed
 *       ({@code geoserver.backend.pgconfig.datasource.jndi-name} is set), since reusing the same
 *       JNDI name is what makes the JDBC quota store share the pgconfig connection pool.
 * </ul>
 *
 * <p>Note on {@code PostgreSQLQuotaDialect}: provided by the upstream
 * {@code geowebcache-diskquota-context.xml} (already imported by
 * {@code DiskQuotaConfiguration}), so this auto-config does not redeclare it.
 *
 * @since 2.28.3.1
 */
@AutoConfiguration(after = {PgconfigDataSourceAutoConfiguration.class, PgconfigMigrationAutoConfiguration.class})
@ConditionalOnDiskQuotaEnabled
@ConditionalOnPgconfigBackendEnabled
@ConditionalOnProperty(prefix = "geoserver.backend.pgconfig.datasource", name = "jndi-name")
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.backend")
public class PgconfigDiskQuotaAutoConfiguration {

    @Bean
    PgconfigDiskQuotaBootstrap pgconfigDiskQuotaBootstrap(
            @Qualifier("DiskQuotaConfigLoader") ConfigLoader configLoader,
            @Qualifier("DiskQuotaConfigResourceProvider") GeoserverXMLResourceProvider diskQuotaConfigResourceProvider,
            @Qualifier("jdbcDiskQuotaConfigResourceProvider")
                    GeoserverXMLResourceProvider jdbcDiskQuotaConfigResourceProvider,
            PgconfigBackendProperties pgconfigProperties) {
        log.info("GeoWebCache DiskQuota will be pre-configured to share the pgconfig JNDI DataSource");
        return new PgconfigDiskQuotaBootstrap(
                configLoader, diskQuotaConfigResourceProvider, jdbcDiskQuotaConfigResourceProvider, pgconfigProperties);
    }
}
