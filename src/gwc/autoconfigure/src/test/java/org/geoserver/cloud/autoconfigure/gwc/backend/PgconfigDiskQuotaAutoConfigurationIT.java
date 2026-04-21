/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gwc.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import javax.sql.DataSource;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.PgconfigBackendAutoConfiguration;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.PgconfigDataSourceAutoConfiguration;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.PgconfigMigrationAutoConfiguration;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.PgconfigTransactionManagerAutoConfiguration;
import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheContextRunner;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.backend.pgconfig.support.PgconfigTestDatabaseSupport;
import org.geoserver.gwc.ConfigurableQuotaStoreProvider;
import org.geowebcache.diskquota.DiskQuotaMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Auto-configuration test for {@link PgconfigDiskQuotaAutoConfiguration}.
 *
 * <p>Verifies the conditional matrix (when the bootstrap bean is/isn't registered) and the
 * end-to-end Flyway migrations against a real Postgres testcontainer. The bootstrap's file-writing
 * behavior is covered separately in {@link PgconfigDiskQuotaBootstrapTest}.
 */
@Testcontainers(disabledWithoutDocker = true)
class PgconfigDiskQuotaAutoConfigurationIT {

    @Container
    static PgConfigTestContainer<?> container = new PgConfigTestContainer<>();

    @RegisterExtension
    PgconfigTestDatabaseSupport db = new PgconfigTestDatabaseSupport(container);

    @TempDir
    File cacheDir;

    private WebApplicationContextRunner runner;

    @BeforeEach
    void setUp() {
        runner = GeoWebCacheContextRunner.newMinimalGeoWebCacheContextRunner(cacheDir)
                .withConfiguration(AutoConfigurations.of(
                        PgconfigTileLayerCatalogAutoConfiguration.class,
                        PgconfigDiskQuotaAutoConfiguration.class,
                        PgconfigBackendAutoConfiguration.class,
                        PgconfigDataSourceAutoConfiguration.class,
                        PgconfigTransactionManagerAutoConfiguration.class,
                        PgconfigMigrationAutoConfiguration.class));
        runner = db.withJdbcUrlConfig(runner);
    }

    @Test
    void registersBootstrapWhenEnabledAndJndiConfigured() {
        WebApplicationContextRunner jndiRunner = GeoWebCacheContextRunner.newMinimalGeoWebCacheContextRunner(cacheDir)
                .withConfiguration(AutoConfigurations.of(
                        PgconfigTileLayerCatalogAutoConfiguration.class,
                        PgconfigDiskQuotaAutoConfiguration.class,
                        PgconfigBackendAutoConfiguration.class,
                        PgconfigDataSourceAutoConfiguration.class,
                        PgconfigTransactionManagerAutoConfiguration.class,
                        PgconfigMigrationAutoConfiguration.class));
        jndiRunner = container.withJndiConfig(jndiRunner);
        jndiRunner.withPropertyValues("gwc.disk-quota.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(PgconfigDiskQuotaBootstrap.class);
            assertThat(context).hasSingleBean(DiskQuotaMonitor.class);
            assertThat(context).hasSingleBean(ConfigurableQuotaStoreProvider.class);
        });
    }

    @Test
    void flywayMigrationCreatesDiskQuotaTables() {
        runner.withPropertyValues("gwc.disk-quota.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();

            DataSource ds = context.getBean("pgconfigDataSource", DataSource.class);
            JdbcTemplate jt = new JdbcTemplate(ds);

            Long flywayRecord = jt.queryForObject(
                    """
                    SELECT count(*) FROM flyway_schema_history
                    WHERE version = ? AND success
                    """,
                    Long.class,
                    "3.1.0");
            assertThat(flywayRecord).isOne();

            Long tilesetCount = jt.queryForObject("SELECT count(*) FROM TILESET", Long.class);
            Long tilepageCount = jt.queryForObject("SELECT count(*) FROM TILEPAGE", Long.class);
            assertThat(tilesetCount).isNotNull();
            assertThat(tilepageCount).isNotNull();
        });
    }

    @Test
    void resourceLockUpgradeMigrationApplied() {
        runner.withPropertyValues("gwc.disk-quota.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();

            DataSource ds = context.getBean("pgconfigDataSource", DataSource.class);
            JdbcTemplate jt = new JdbcTemplate(ds);

            Long expiredAfterColumn = jt.queryForObject(
                    """
                    SELECT count(*) FROM information_schema.columns
                    WHERE table_schema = ?
                      AND upper(table_name) = 'RESOURCE_LOCK'
                      AND upper(column_name) = 'EXPIRED_AFTER'
                    """,
                    Long.class,
                    db.getSchema());
            assertThat(expiredAfterColumn).isOne();
        });
    }

    /**
     * With {@code gwc.disk-quota.enabled=false} (default), {@link DiskQuotaMonitor} and the upstream
     * {@code ConfigurableQuotaStoreProvider} are still in the context (gwcFacade and the Wicket UI
     * need them) but {@code DiskQuotaConfiguration#diskQuotaDisabledPropertySetter} sets
     * {@code GWC_DISKQUOTA_DISABLED=true} so neither does anything at runtime. The bootstrap is
     * also off.
     */
    @Test
    void disabledByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(PgconfigDiskQuotaBootstrap.class);
            assertThat(context).hasSingleBean(DiskQuotaMonitor.class);
            assertThat(context).hasSingleBean(ConfigurableQuotaStoreProvider.class);
            assertThat(context.getBean(DiskQuotaMonitor.class).isEnabled()).isFalse();
        });
    }

    @Test
    void skipsWhenPgconfigBackendDisabled() {
        runner.withPropertyValues(
                        "gwc.disk-quota.enabled=true",
                        "geoserver.backend.pgconfig.enabled=false",
                        "geoserver.backend.pgconfig.datasource.jndi-name=java:comp/env/jdbc/pgconfig")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(PgconfigDiskQuotaBootstrap.class);
                });
    }

    /**
     * The bootstrap is off when pgconfig is configured without a JNDI name (direct JDBC URL): the
     * point of the bootstrap is to point DiskQuota at the same JNDI source pgconfig uses, so it has
     * nothing to do when there isn't one.
     */
    @Test
    void skipsWhenPgconfigJndiNotConfigured() {
        runner.withPropertyValues("gwc.disk-quota.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(PgconfigDiskQuotaBootstrap.class);
        });
    }
}
