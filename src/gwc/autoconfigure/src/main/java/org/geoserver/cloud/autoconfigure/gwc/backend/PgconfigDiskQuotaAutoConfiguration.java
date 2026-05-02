/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.backend;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.ConditionalOnPgconfigBackendEnabled;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.PgconfigDataSourceAutoConfiguration;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.PgconfigMigrationAutoConfiguration;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnDiskQuotaEnabled;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigBackendProperties;
import org.geoserver.cloud.gwc.backend.pgconfig.PgconfigQuotaStoreProvider;
import org.geoserver.gwc.ConfigurableQuotaStoreProvider;
import org.geoserver.gwc.JDBCConfigurationStorage;
import org.geowebcache.diskquota.ConfigLoader;
import org.geowebcache.diskquota.QuotaStore;
import org.geowebcache.diskquota.jdbc.JDBCQuotaStore;
import org.geowebcache.diskquota.jdbc.PostgreSQLDialect;
import org.geowebcache.diskquota.jdbc.SQLDialect;
import org.geowebcache.diskquota.storage.TilePageCalculator;
import org.geowebcache.storage.DefaultStorageFinder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Auto-configuration that wires GeoWebCache's JDBC disk-quota store to the {@code pgconfigDataSource} provided by the
 * pgconfig catalog backend.
 *
 * <p>Activates only when:
 *
 * <ul>
 *   <li>{@link ConditionalOnDiskQuotaEnabled gwc.disk-quota.enabled=true}, and
 *   <li>{@link ConditionalOnPgconfigBackendEnabled geoserver.backend.pgconfig.enabled=true}, and
 *   <li>{@link PgconfigQuotaStoreProvider} is on the classpath (i.e. the {@code gwc-cloud-catalog-pgconfig} module is
 *       present).
 * </ul>
 *
 * <p>Beans contributed:
 *
 * <ul>
 *   <li>{@code PostgreSQLQuotaDialect} - the dialect bean keyed by the upstream factory's {@code dialectName +
 *       "QuotaDialect"} convention.
 *   <li>{@code pgconfigQuotaStore} - a {@link JDBCQuotaStore} initialized against the pgconfig DataSource. The
 *       DataSource is wrapped with {@link DelegatingDataSource} so {@link JDBCQuotaStore#close()} cannot close the
 *       shared pool (its {@code instanceof Closeable} branch is bypassed).
 *   <li>{@code DiskQuotaStoreProvider} - a {@link PgconfigQuotaStoreProvider} (a {@link ConfigurableQuotaStoreProvider}
 *       subclass) that returns the pre-built store. The bean id matches the one referenced by the upstream
 *       {@code DiskQuotaMonitor} XML definition; extending {@link ConfigurableQuotaStoreProvider} keeps the GeoServer
 *       Wicket UI's {@code getBeanOfType} lookup working.
 * </ul>
 *
 * <p>The Flyway migration {@code V3_1_0__DiskQuota_Tables.sql} (in the {@code gs-cloud-catalog-backend-pgconfig}
 * module) creates the {@code TILESET}/{@code TILEPAGE} tables in the pgconfig schema before this auto-config runs, so
 * {@link JDBCQuotaStore#initialize()} skips its own DDL.
 *
 * @since 3.0.0
 */
@AutoConfiguration(after = {PgconfigDataSourceAutoConfiguration.class, PgconfigMigrationAutoConfiguration.class})
@ConditionalOnClass(PgconfigQuotaStoreProvider.class)
@ConditionalOnDiskQuotaEnabled
@ConditionalOnPgconfigBackendEnabled
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.gwc.backend")
public class PgconfigDiskQuotaAutoConfiguration {

    @PostConstruct
    void log() {
        log.info("GeoWebCache DiskQuota using PostgreSQL JDBC store on the pgconfig DataSource");
    }

    @Bean(name = "PostgreSQLQuotaDialect")
    SQLDialect postgreSQLQuotaDialect() {
        return new PostgreSQLDialect();
    }

    @Bean(name = "pgconfigQuotaStore", destroyMethod = "")
    QuotaStore pgconfigQuotaStore(
            @Qualifier("pgconfigDataSource") DataSource pgconfigDataSource,
            @Qualifier("gwcDefaultStorageFinder") DefaultStorageFinder storageFinder,
            @Qualifier("gwcTilePageCalculator") TilePageCalculator tilePageCalculator,
            @Qualifier("PostgreSQLQuotaDialect") SQLDialect dialect,
            PgconfigBackendProperties pgconfigProperties) {

        DataSource nonClosingDataSource = new DelegatingDataSource(pgconfigDataSource);
        JDBCQuotaStore store = new JDBCQuotaStore(storageFinder, tilePageCalculator);
        store.setDataSource(nonClosingDataSource);
        store.setDialect(dialect);
        store.setSchema(pgconfigProperties.getSchema());
        store.initialize();
        return store;
    }

    @Bean(name = "DiskQuotaStoreProvider")
    ConfigurableQuotaStoreProvider diskQuotaStoreProvider(
            @Qualifier("DiskQuotaConfigLoader") ConfigLoader loader,
            @Qualifier("gwcTilePageCalculator") TilePageCalculator tilePageCalculator,
            @Qualifier("gwcJdbcConfigurationStorage") JDBCConfigurationStorage jdbcConfigStorage,
            @Qualifier("pgconfigQuotaStore") QuotaStore store) {
        return new PgconfigQuotaStoreProvider(loader, tilePageCalculator, jdbcConfigStorage, store);
    }
}
