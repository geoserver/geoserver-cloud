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
import org.geoserver.cloud.gwc.backend.pgconfig.PgconfigQuotaStoreProvider;
import org.geoserver.gwc.ConfigurableQuotaStoreProvider;
import org.geowebcache.diskquota.DiskQuotaMonitor;
import org.geowebcache.diskquota.QuotaStore;
import org.geowebcache.diskquota.jdbc.JDBCQuotaStore;
import org.geowebcache.diskquota.jdbc.PostgreSQLDialect;
import org.geowebcache.diskquota.jdbc.SQLDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for {@link PgconfigDiskQuotaAutoConfiguration}.
 *
 * <p>Verifies the conditional matrix and, when active, the JDBC quota store wiring against the pgconfig DataSource plus
 * the Flyway-applied DDL.
 */
@Testcontainers(disabledWithoutDocker = true)
@Execution(value = ExecutionMode.SAME_THREAD) // FilteringXmlBeanDefinitionReader has a static HashMap race condition
class PgconfigDiskQuotaAutoConfigurationIT {

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

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
    void wiresJdbcQuotaStoreOnPgconfigDataSourceWhenEnabled() {
        runner.withPropertyValues("gwc.disk-quota.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();

            assertThat(context)
                    .getBean("PostgreSQLQuotaDialect", SQLDialect.class)
                    .isExactlyInstanceOf(PostgreSQLDialect.class);

            assertThat(context).getBean("pgconfigQuotaStore", QuotaStore.class).isInstanceOf(JDBCQuotaStore.class);

            // Bean id used by the upstream DiskQuotaMonitor XML definition.
            assertThat(context)
                    .getBean("DiskQuotaStoreProvider", ConfigurableQuotaStoreProvider.class)
                    .isInstanceOf(PgconfigQuotaStoreProvider.class);

            // The Wicket UI's DiskQuotaWarningPanel looks up the provider by type.
            assertThat(context).hasSingleBean(ConfigurableQuotaStoreProvider.class);
            assertThat(context).hasSingleBean(DiskQuotaMonitor.class);

            ConfigurableQuotaStoreProvider provider =
                    context.getBean("DiskQuotaStoreProvider", ConfigurableQuotaStoreProvider.class);
            assertThat(provider.getQuotaStore()).isSameAs(context.getBean("pgconfigQuotaStore"));
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
    void jdbcQuotaStoreInitializesGlobalQuotaRow() {
        runner.withPropertyValues("gwc.disk-quota.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();

            DataSource ds = context.getBean("pgconfigDataSource", DataSource.class);
            JdbcTemplate jt = new JdbcTemplate(ds);

            Long globalQuota = jt.queryForObject(
                    "SELECT count(*) FROM TILESET WHERE KEY = ?", Long.class, JDBCQuotaStore.GLOBAL_QUOTA_NAME);
            assertThat(globalQuota).isOne();
        });
    }

    /**
     * With {@code gwc.disk-quota.enabled=false} (default), {@link DiskQuotaMonitor} and the upstream
     * {@code ConfigurableQuotaStoreProvider} are still in the context (gwcFacade and the Wicket UI need them) but
     * {@code DiskQuotaAutoConfiguration#diskQuotaDisabledPropertySetter} sets {@code GWC_DISKQUOTA_DISABLED=true} so
     * neither does anything at runtime.
     */
    @Test
    void disabledByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean("pgconfigQuotaStore");
            assertThat(context).doesNotHaveBean(PgconfigQuotaStoreProvider.class);
            assertThat(context).hasSingleBean(DiskQuotaMonitor.class);
            // Fallback ConfigurableQuotaStoreProvider keeps the Wicket UI happy.
            assertThat(context).hasSingleBean(ConfigurableQuotaStoreProvider.class);
            assertThat(context.getBean(DiskQuotaMonitor.class).isEnabled()).isFalse();
        });
    }

    @Test
    void pgconfigAutoConfigSkipsWhenPgconfigBackendDisabled() {
        runner.withPropertyValues("geoserver.backend.pgconfig.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean("pgconfigQuotaStore");
            assertThat(context).doesNotHaveBean(PgconfigQuotaStoreProvider.class);
            assertThat(context).doesNotHaveBean(JDBCQuotaStore.class);
        });
    }

    @Test
    void pgconfigAutoConfigSkipsWhenBackendModuleNotOnClasspath() {
        runner.withClassLoader(new FilteredClassLoader(PgconfigQuotaStoreProvider.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("pgconfigQuotaStore");
                    assertThat(context).doesNotHaveBean(PgconfigQuotaStoreProvider.class);
                    assertThat(context).doesNotHaveBean(JDBCQuotaStore.class);
                });
    }
}
