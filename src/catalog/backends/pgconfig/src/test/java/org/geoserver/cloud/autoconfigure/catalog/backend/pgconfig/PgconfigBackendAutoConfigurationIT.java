/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import javax.sql.DataSource;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.ConfigInfoKeys;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheBusListener;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheEventListener;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheLifecycle;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheMetrics;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCodec;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup;
import org.geoserver.cloud.backend.pgconfig.cache.NoopCodec;
import org.geoserver.cloud.backend.pgconfig.cache.ZstdCodec;
import org.geoserver.cloud.backend.pgconfig.catalog.PgconfigCatalogFacade;
import org.geoserver.cloud.backend.pgconfig.config.PgconfigConfigRepository;
import org.geoserver.cloud.backend.pgconfig.config.PgconfigGeoServerFacade;
import org.geoserver.cloud.backend.pgconfig.config.PgconfigUpdateSequence;
import org.geoserver.cloud.backend.pgconfig.resource.PgconfigLockProvider;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigGeoServerLoader;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigGeoServerResourceLoader;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.amqp.rabbit.listener.ConsumeOkEvent;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Test suite for {@link PgconfigBackendAutoConfiguration}
 *
 * @since 1.4
 */
@Testcontainers(disabledWithoutDocker = true)
@Execution(value = ExecutionMode.CONCURRENT)
class PgconfigBackendAutoConfigurationIT {

    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer();

    private ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean("securityManager", GeoServerSecurityManager.class, () -> mock(GeoServerSecurityManager.class))
            .withConfiguration(AutoConfigurations.of(
                    PgconfigDataSourceAutoConfiguration.class,
                    PgconfigTransactionManagerAutoConfiguration.class,
                    PgconfigMigrationAutoConfiguration.class,
                    PgconfigBackendAutoConfiguration.class));

    @BeforeEach
    void setUp() {
        runner = container.withJdbcUrlConfig(runner);
    }

    @Test
    void testCatalogAndConfigBeans() {
        runner.run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasBean("pgconfigTransactionManager")
                    .hasSingleBean(JdbcTemplate.class)
                    .hasSingleBean(GeoServerConfigurationLock.class)
                    .hasSingleBean(PgconfigUpdateSequence.class)
                    .hasSingleBean(PgconfigCatalogFacade.class)
                    .hasSingleBean(PgconfigGeoServerLoader.class)
                    .hasSingleBean(PgconfigConfigRepository.class)
                    .hasSingleBean(PgconfigGeoServerFacade.class)
                    .hasBean("resourceStoreImpl")
                    .hasSingleBean(PgconfigGeoServerResourceLoader.class)
                    .hasSingleBean(PgconfigLockProvider.class);

            ExtendedCatalogFacade catalogFacade = context.getBean("catalogFacade", ExtendedCatalogFacade.class);
            PgconfigCatalogFacade pgconfigFacade = context.getBean(PgconfigCatalogFacade.class);
            assertThat(catalogFacade)
                    .isInstanceOf(ResolvingCatalogFacadeDecorator.class)
                    .extracting(facade -> ((ResolvingCatalogFacadeDecorator) facade).getSubject())
                    .isSameAs(pgconfigFacade);
        });
    }

    @Test
    void catalogCacheIsWired() {
        runner.run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasBean("pgconfigCatalogEntityCache")
                    .hasBean("pgconfigCatalogEntityCacheMetrics")
                    .hasSingleBean(EntityCacheEventListener.class)
                    .hasSingleBean(EntityCacheLifecycle.class)
                    .hasSingleBean(EntityCacheBusListener.class)
                    .getBean("pgconfigCatalogCacheCodec")
                    .isInstanceOf(ZstdCodec.class);
            EntityCache cache = context.getBean("pgconfigCatalogEntityCache", EntityCache.class);
            ExtendedCatalogFacade facade = context.getBean("catalogFacade", ExtendedCatalogFacade.class);
            assertThat(cache.name()).isEqualTo("gs-catalog");
            assertThat(lookupAfterAdd(facade, cache)).isInstanceOf(Lookup.Hit.class);
        });
    }

    // the facade reads the workspace back after adding it, and the read fills the cache
    private Lookup lookupAfterAdd(ExtendedCatalogFacade facade, EntityCache cache) {
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId("WorkspaceInfo-" + UUID.randomUUID());
        workspace.setName("ws-" + UUID.randomUUID());
        facade.add(workspace);
        return cache.lookup(CacheKey.entity(ConfigInfoType.WORKSPACE, workspace.getId()));
    }

    // the configuration facade finds the workspace of a service in the catalog facade, not in the catalog
    @Test
    void serviceWorkspaceFromFacade() {
        runner.run(context -> {
            ExtendedCatalogFacade catalogFacade = context.getBean("catalogFacade", ExtendedCatalogFacade.class);
            PgconfigGeoServerFacade configFacade = context.getBean(PgconfigGeoServerFacade.class);
            Catalog catalog = mock(Catalog.class);
            GeoServerImpl geoServer = new GeoServerImpl(configFacade);
            geoServer.setCatalog(catalog);
            WorkspaceInfo workspace = addWorkspace(catalogFacade);
            WMSInfoImpl service = new WMSInfoImpl();
            service.setName("wms");
            service.setWorkspace(workspace);
            configFacade.add(service);

            WMSInfo found = configFacade.getService(workspace, WMSInfo.class);

            assertThat(found.getWorkspace()).extracting(WorkspaceInfo::getId).isEqualTo(workspace.getId());
            verifyNoInteractions(catalog);
        });
    }

    private WorkspaceInfo addWorkspace(ExtendedCatalogFacade facade) {
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setId("WorkspaceInfo-" + UUID.randomUUID());
        workspace.setName("ws-" + UUID.randomUUID());
        return facade.add(workspace);
    }

    @Test
    void configCacheIsWired() {
        runner.run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasBean("pgconfigConfigEntityCache")
                    .hasBean("pgconfigConfigEntityCacheMetrics");
            EntityCache cache = context.getBean("pgconfigConfigEntityCache", EntityCache.class);
            PgconfigConfigRepository repository = context.getBean(PgconfigConfigRepository.class);
            assertThat(cache.name()).isEqualTo("gs-config");
            assertThat(lookupAfterSet(repository, cache)).isInstanceOf(Lookup.Hit.class);
        });
    }

    // the global settings are read back after they are set, and the read fills the cache
    private Lookup lookupAfterSet(PgconfigConfigRepository repository, EntityCache cache) {
        repository.setGlobal(new GeoServerInfoImpl());
        repository.getGlobal();
        return cache.lookup(ConfigInfoKeys.global());
    }

    // the update sequence reads the global settings within a transaction on the pgconfig data source, and the
    // repository reads the database within that transaction only if its template runs on the same data source
    @Test
    void templateSharesDataSource() {
        runner.run(context -> {
            DataSource dataSource = context.getBean("pgconfigDataSource", DataSource.class);
            JdbcTemplate template = context.getBean("pcconfigJdbcTemplate", JdbcTemplate.class);

            assertThat(template.getDataSource()).isSameAs(dataSource);
        });
    }

    @Test
    void lifecycleSuspendsBothCaches() {
        runner.run(context -> {
            EntityCache catalogCache = context.getBean("pgconfigCatalogEntityCache", EntityCache.class);
            EntityCache configCache = context.getBean("pgconfigConfigEntityCache", EntityCache.class);

            context.getBean(EntityCacheLifecycle.class).beforeReload();

            assertThat(catalogCache.isSuspended()).isTrue();
            assertThat(configCache.isSuspended()).isTrue();
        });
    }

    // the listener receives the events of other service instances published in the context
    @Test
    void remoteEventEvictsEntry() {
        runner.run(context -> {
            EntityCache cache = context.getBean("pgconfigCatalogEntityCache", EntityCache.class);
            WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
            workspace.setId("ws-1");
            workspace.setName("topp");
            CacheKey key = CacheKey.entity(ConfigInfoType.WORKSPACE, "ws-1");
            cache.put(key, "{}".getBytes(UTF_8), cache.epoch());
            CatalogInfoRemoved removed = CatalogInfoRemoved.createLocal(1L, workspace);
            removed.setRemote(true);

            context.publishEvent(removed);

            assertThat(cache.lookup(key)).isInstanceOf(Lookup.Absent.class);
        });
    }

    @Test
    void busSubscriptionClearsCache() {
        runner.run(context -> {
            EntityCache cache = context.getBean("pgconfigCatalogEntityCache", EntityCache.class);
            CacheKey key = CacheKey.entity(ConfigInfoType.WORKSPACE, "ws-1");
            cache.put(key, "{}".getBytes(UTF_8), cache.epoch());

            context.publishEvent(new ConsumeOkEvent(this, "geoserver.anonymous.queue", "consumer-1"));

            assertThat(cache.lookup(key)).isInstanceOf(Lookup.Absent.class);
        });
    }

    @Test
    void noCompressionIsNoopCodec() {
        runner.withPropertyValues("geoserver.backend.pgconfig.cache.compression=none")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .getBean("pgconfigCatalogCacheCodec")
                        .isInstanceOf(NoopCodec.class));
    }

    @Test
    void disabledCacheHasNoBean() {
        runner.withPropertyValues("geoserver.backend.pgconfig.cache.enabled=false")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(PgconfigCatalogFacade.class)
                        .hasSingleBean(PgconfigConfigRepository.class)
                        .doesNotHaveBean(EntityCache.class)
                        .doesNotHaveBean(EntityCodec.class)
                        .doesNotHaveBean(EntityCacheMetrics.class)
                        .doesNotHaveBean(EntityCacheEventListener.class)
                        .doesNotHaveBean(EntityCacheLifecycle.class)
                        .doesNotHaveBean(EntityCacheBusListener.class));
    }

    // micrometer is an optional dependency of the module
    @Test
    void startsWithoutMicrometer() {
        runner.withClassLoader(new FilteredClassLoader(MeterRegistry.class)).run(context -> assertThat(context)
                .hasNotFailed()
                .hasBean("pgconfigCatalogEntityCache")
                .hasBean("pgconfigConfigEntityCache")
                .doesNotHaveBean(EntityCacheMetrics.class));
    }

    // Spring AMQP is an optional dependency of the module
    @Test
    void startsWithoutSpringRabbit() {
        runner.withClassLoader(new FilteredClassLoader(ConsumeOkEvent.class)).run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(EntityCacheEventListener.class)
                .hasSingleBean(EntityCacheLifecycle.class)
                .doesNotHaveBean(EntityCacheBusListener.class));
    }
}
