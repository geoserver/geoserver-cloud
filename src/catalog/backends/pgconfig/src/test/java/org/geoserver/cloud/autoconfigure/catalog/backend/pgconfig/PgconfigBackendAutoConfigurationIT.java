/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.forwarding.ResolvingCatalogFacadeDecorator;
import org.geoserver.catalog.plugin.requestcache.RemoteChangeCounter;
import org.geoserver.catalog.plugin.requestcache.RequestCache;
import org.geoserver.catalog.plugin.requestcache.RequestCacheBudget;
import org.geoserver.catalog.plugin.requestcache.RequestCacheScope;
import org.geoserver.catalog.plugin.requestcache.RequestCacheThreadLocalTransfer;
import org.geoserver.catalog.plugin.requestcache.RequestCachingCatalogFacade;
import org.geoserver.catalog.plugin.requestcache.RequestCachingGeoServerFacade;
import org.geoserver.cloud.autoconfigure.catalog.backend.core.GeoServerBackendAutoConfiguration;
import org.geoserver.cloud.backend.pgconfig.cache.CacheKey;
import org.geoserver.cloud.backend.pgconfig.cache.ConfigInfoKeys;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheEventListener;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheLifecycle;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheMetrics;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheProperties;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCodec;
import org.geoserver.cloud.backend.pgconfig.cache.Lookup;
import org.geoserver.cloud.backend.pgconfig.cache.NoopCodec;
import org.geoserver.cloud.backend.pgconfig.cache.RequestCacheEventListener;
import org.geoserver.cloud.backend.pgconfig.cache.ZstdCodec;
import org.geoserver.cloud.backend.pgconfig.catalog.PgconfigCatalogFacade;
import org.geoserver.cloud.backend.pgconfig.catalog.PgconfigLayerGroupWarmUp;
import org.geoserver.cloud.backend.pgconfig.config.PgconfigConfigRepository;
import org.geoserver.cloud.backend.pgconfig.config.PgconfigGeoServerFacade;
import org.geoserver.cloud.backend.pgconfig.config.PgconfigUpdateSequence;
import org.geoserver.cloud.backend.pgconfig.resource.PgconfigLockProvider;
import org.geoserver.cloud.backend.pgconfig.resource.PgconfigResourceStore;
import org.geoserver.cloud.backend.pgconfig.resource.ResourceChangeListener;
import org.geoserver.cloud.backend.pgconfig.resource.ResourceStoreCache;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigGeoServerLoader;
import org.geoserver.cloud.config.catalog.backend.pgconfig.PgconfigGeoServerResourceLoader;
import org.geoserver.cloud.config.catalog.backend.pgconfig.RequestCacheFilter;
import org.geoserver.cloud.config.catalog.backend.pgconfig.RequestCacheMetrics;
import org.geoserver.cloud.event.bus.RemoteEventsResumed;
import org.geoserver.cloud.event.catalog.CatalogInfoRemoved;
import org.geoserver.cloud.event.info.ConfigInfoType;
import org.geoserver.cloud.event.resource.ResourceChanged;
import org.geoserver.cloud.event.resource.ResourceChanged.Kind;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.plugin.GeoServerImpl;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GsCloudLayerGroupContainmentCache;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.ResourceLockTarget;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Test suite for {@link PgconfigBackendAutoConfiguration}
 *
 * @since 1.4
 */
@Testcontainers(disabledWithoutDocker = true)
@Execution(value = ExecutionMode.CONCURRENT)
@ResourceLock(
        value = PgconfigBackendAutoConfigurationIT.SHARED_BUDGET,
        mode = ResourceAccessMode.READ,
        target = ResourceLockTarget.CHILDREN)
class PgconfigBackendAutoConfigurationIT {

    /** The lock of the tests changing the global settings: the tests of this class share one database. */
    private static final String GLOBAL_SETTINGS = "pgconfig global settings";

    /** The lock of the request cache budget: the context of each test sets the budget shared by the JVM. */
    static final String SHARED_BUDGET = "request cache budget of the JVM";

    // the tests run at the same time, and the context of each test opens a pool of 10 connections
    @Container
    static PgConfigTestContainer container = new PgConfigTestContainer().withMaxConnections(200);

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

            // the cache of the request, over the resolving decorator, over the facade on the database
            CatalogFacade catalogFacade = context.getBean("catalogFacade", CatalogFacade.class);
            PgconfigCatalogFacade pgconfigFacade = context.getBean(PgconfigCatalogFacade.class);
            assertThat(catalogFacade).isInstanceOf(RequestCachingCatalogFacade.class);
            RequestCachingCatalogFacade requestCaching = (RequestCachingCatalogFacade) catalogFacade;
            CatalogFacade resolving = requestCaching.getSubject();
            assertThat(resolving).isInstanceOf(ResolvingCatalogFacadeDecorator.class);
            ResolvingCatalogFacadeDecorator decorator = (ResolvingCatalogFacadeDecorator) resolving;
            assertThat(decorator.getSubject()).isSameAs(pgconfigFacade);
        });
    }

    @Test
    void requestCacheIsWired() {
        runner.run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(RequestCacheThreadLocalTransfer.class)
                    .hasSingleBean(RequestCacheMetrics.class);
            EntityCacheProperties properties = context.getBean(EntityCacheProperties.class);
            assertThat(properties.getRequest().getMaxEntries()).isEqualTo(8_000);

            GeoServerFacade geoserverFacade = context.getBean("geoserverFacade", GeoServerFacade.class);
            PgconfigGeoServerFacade pgconfigFacade = context.getBean(PgconfigGeoServerFacade.class);
            assertThat(geoserverFacade).isInstanceOf(RequestCachingGeoServerFacade.class);
            RequestCachingGeoServerFacade requestCaching = (RequestCachingGeoServerFacade) geoserverFacade;
            assertThat(requestCaching.getSubject()).isSameAs(pgconfigFacade);

            // before Spring Security at -100 and the GeoServer filters at 1 to 5
            FilterRegistrationBean<?> registration =
                    context.getBean("pgconfigRequestCacheFilter", FilterRegistrationBean.class);
            assertThat(registration.getFilter()).isInstanceOf(RequestCacheFilter.class);
            assertThat(registration.getFilterName()).isEqualTo("pgconfigRequestCacheFilter");
            assertThat(registration.getUrlPatterns()).containsExactly("/*");
            assertThat(registration.getOrder()).isEqualTo(-1000);
        });
    }

    // the cache of a request opened by the filter takes one entry, the second put is rejected
    @Test
    void filterTakesMaxEntries() {
        runner.withPropertyValues("geoserver.backend.pgconfig.cache.request.max-entries=1")
                .run(context -> {
                    FilterRegistrationBean<?> registration =
                            context.getBean("pgconfigRequestCacheFilter", FilterRegistrationBean.class);
                    Filter filter = registration.getFilter();
                    List<RequestCache> requestCaches = new ArrayList<>();
                    FilterChain chain = (request, response) -> requestCaches.add(putTwoEntries());

                    filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

                    assertThat(requestCaches)
                            .singleElement()
                            .extracting(RequestCache::rejected)
                            .isEqualTo(1L);
                });
    }

    // the context of any other test of the class resets the value read by this one
    @Test
    @ResourceLock(value = SHARED_BUDGET, mode = ResourceAccessMode.READ_WRITE)
    void budgetTakesMaxTotalEntries() {
        try {
            runner.withPropertyValues("geoserver.backend.pgconfig.cache.request.max-total-entries=7")
                    .run(context -> {
                        RequestCacheBudget budget = context.getBean(RequestCacheBudget.class);
                        assertThat(budget).isSameAs(RequestCacheBudget.shared());
                        assertThat(budget.maxTotalEntries()).isEqualTo(7);
                    });
        } finally {
            RequestCacheBudget.shared().setMaxTotalEntries(RequestCacheBudget.DEFAULT_MAX_TOTAL_ENTRIES);
        }
    }

    private static RequestCache putTwoEntries() {
        RequestCache cache = RequestCacheScope.current();
        cache.put("roads", "the roads layer");
        cache.put("rivers", "the rivers layer");
        return cache;
    }

    @Test
    void disabledRequestCacheHasNoBean() {
        runner.withPropertyValues("geoserver.backend.pgconfig.cache.request.enabled=false")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .doesNotHaveBean("pgconfigRequestCacheFilter")
                            .doesNotHaveBean(RequestCacheThreadLocalTransfer.class)
                            .doesNotHaveBean(RequestCacheMetrics.class)
                            .doesNotHaveBean(RequestCacheBudget.class)
                            .doesNotHaveBean(RequestCacheEventListener.class);
                    ResolvingCatalogFacadeDecorator catalogFacade =
                            context.getBean("catalogFacade", ResolvingCatalogFacadeDecorator.class);
                    PgconfigCatalogFacade pgconfigCatalogFacade = context.getBean(PgconfigCatalogFacade.class);
                    Object geoserverFacade = context.getBean("geoserverFacade");
                    Object pgconfigGeoServerFacade = context.getBean("pgconfigGeoServerFacade");

                    assertThat(catalogFacade.getSubject()).isSameAs(pgconfigCatalogFacade);
                    assertThat(geoserverFacade).isSameAs(pgconfigGeoServerFacade);
                });
    }

    // the switch reads 1 as true, as the bound properties do, for the facades and for the other beans of the cache
    @Test
    void requestCacheEnabledByOne() {
        runner.withPropertyValues("geoserver.backend.pgconfig.cache.request.enabled=1")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasBean("pgconfigRequestCacheFilter")
                            .hasSingleBean(RequestCacheThreadLocalTransfer.class)
                            .hasSingleBean(RequestCacheMetrics.class);
                    Object catalogFacade = context.getBean("catalogFacade");
                    Object geoserverFacade = context.getBean("geoserverFacade");
                    EntityCacheProperties properties = context.getBean(EntityCacheProperties.class);

                    assertThat(catalogFacade).isInstanceOf(RequestCachingCatalogFacade.class);
                    assertThat(geoserverFacade).isInstanceOf(RequestCachingGeoServerFacade.class);
                    assertThat(properties.getRequest().isEnabled()).isTrue();
                });
    }

    // a copy of the global settings read earlier by the request misses the change of another service instance, and
    // saving the new value from that copy would undo the change
    @Test
    @ResourceLock(GLOBAL_SETTINGS)
    void sequenceSkipsRequestCache() {
        runner.run(context -> {
            GeoServer geoServer = geoServerOf(context);
            PgconfigUpdateSequence sequence = context.getBean(PgconfigUpdateSequence.class);
            GeoServer otherInstance = otherInstance(context);
            geoServer.setGlobal(new GeoServerInfoImpl());

            RequestCacheScope.open(100);
            try {
                geoServer.getGlobal();
                changeTitle(otherInstance, "changed elsewhere");
                sequence.nextValue();
            } finally {
                RequestCacheScope.close();
            }

            GeoServerInfo stored = otherInstance.getGlobal();
            assertThat(stored.getSettings().getTitle()).isEqualTo("changed elsewhere");
        });
    }

    /** The GeoServer of the service instance, built over the configuration facade as the core configuration does. */
    private GeoServer geoServerOf(ApplicationContext context) {
        GeoServerFacade facade = context.getBean("geoserverFacade", GeoServerFacade.class);
        return new GeoServerImpl(facade);
    }

    /** Another service instance on the same database, without caches. */
    private GeoServer otherInstance(ApplicationContext context) {
        DataSource dataSource = context.getBean("pgconfigDataSource", DataSource.class);
        PgconfigGeoServerFacade facade = new PgconfigGeoServerFacade(new JdbcTemplate(dataSource));
        return new GeoServerImpl(facade);
    }

    private void changeTitle(GeoServer geoServer, String title) {
        GeoServerInfo global = geoServer.getGlobal();
        global.getSettings().setTitle(title);
        geoServer.save(global);
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
                    .hasBean("pgconfigEntityCacheBusListener")
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
    @ResourceLock(GLOBAL_SETTINGS)
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

    // the counter of the JVM is shared like the budget: the other tests publish remote events too
    @Test
    @ResourceLock(value = SHARED_BUDGET, mode = ResourceAccessMode.READ_WRITE)
    void remoteEventMovesCounter() {
        runner.run(context -> {
            WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
            workspace.setId("ws-1");
            workspace.setName("topp");
            CatalogInfoRemoved removed = CatalogInfoRemoved.createLocal(1L, workspace);
            removed.setRemote(true);
            long before = RemoteChangeCounter.shared().count();

            context.publishEvent(removed);

            assertThat(RemoteChangeCounter.shared().count()).isEqualTo(before + 1);
        });
    }

    @Test
    void busSubscriptionClearsCache() {
        runner.run(context -> {
            EntityCache cache = context.getBean("pgconfigCatalogEntityCache", EntityCache.class);
            CacheKey key = CacheKey.entity(ConfigInfoType.WORKSPACE, "ws-1");
            cache.put(key, "{}".getBytes(UTF_8), cache.epoch());

            context.publishEvent(new RemoteEventsResumed("geoserver.anonymous.queue"));

            assertThat(cache.lookup(key)).isInstanceOf(Lookup.Absent.class);
        });
    }

    // the build of the containment cache runs the warm-up before it lists the groups
    @Test
    void containmentBuildWarmsUpGroups() {
        runner.withPropertyValues("geoserver.security.layergroup-containmentcache=true")
                .withConfiguration(AutoConfigurations.of(GeoServerBackendAutoConfiguration.class))
                // the GeoServer bean of the core configuration takes the secured catalog, absent from this context
                .withBean("catalog", Catalog.class, () -> mock(Catalog.class))
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(PgconfigLayerGroupWarmUp.class)
                            .getBean("layerGroupContainmentCache")
                            .isInstanceOf(GsCloudLayerGroupContainmentCache.class);
                    EntityCache cache = context.getBean("pgconfigCatalogEntityCache", EntityCache.class);
                    CacheKey groupKey = addGroup(context.getBean(PgconfigCatalogFacade.class));

                    // empties the entity cache, then builds the containment cache again
                    context.publishEvent(new RemoteEventsResumed("geoserver.anonymous.queue"));

                    await().atMost(Duration.ofSeconds(10)).until(() -> cache.contains(groupKey));
                });
    }

    // a reset empties the entity cache, and the warm-up loads the groups again in the background
    @Test
    void resetWarmsUpGroups() {
        runner.withPropertyValues("geoserver.security.layergroup-containmentcache=true")
                .run(context -> {
                    EntityCache cache = context.getBean("pgconfigCatalogEntityCache", EntityCache.class);
                    CacheKey groupKey = addGroup(context.getBean(PgconfigCatalogFacade.class));

                    context.getBean(EntityCacheLifecycle.class).onReset();

                    await().atMost(Duration.ofSeconds(10)).until(() -> cache.contains(groupKey));
                });
    }

    // the services without the containment cache, such as wfs, do not load the groups
    @Test
    void noWarmUpByDefault() {
        runner.run(this::assertNoWarmUp);
    }

    @Test
    void noWarmUpWithContainmentOff() {
        runner.withPropertyValues("geoserver.security.layergroup-containmentcache=false")
                .run(this::assertNoWarmUp);
    }

    /** Asserts that the context has no warm-up of the groups, and that a reset leaves a group out of the cache. */
    private void assertNoWarmUp(AssertableApplicationContext context) {
        assertThat(context).hasNotFailed().doesNotHaveBean(PgconfigLayerGroupWarmUp.class);
        EntityCache cache = context.getBean("pgconfigCatalogEntityCache", EntityCache.class);
        CacheKey groupKey = addGroup(context.getBean(PgconfigCatalogFacade.class));

        context.getBean(EntityCacheLifecycle.class).onReset();

        // the warm-up after a reset puts a group within milliseconds
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(2)).until(() -> !cache.contains(groupKey));
    }

    /** Adds a group without members, and returns its key in the entity cache. */
    private CacheKey addGroup(ExtendedCatalogFacade facade) {
        LayerGroupInfoImpl group = new LayerGroupInfoImpl();
        group.setId("LayerGroupInfo-" + UUID.randomUUID());
        group.setName("lg-" + UUID.randomUUID());
        facade.add(group);
        return CacheKey.entity(ConfigInfoType.LAYERGROUP, group.getId());
    }

    @Test
    void noCompressionIsNoopCodec() {
        runner.withPropertyValues("geoserver.backend.pgconfig.cache.compression=none")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .getBean("pgconfigCatalogCacheCodec")
                        .isInstanceOf(NoopCodec.class));
    }

    // the containment cache on: without the entity cache, it gets no warm-up of the groups
    @Test
    void disabledCacheHasNoBean() {
        runner.withPropertyValues(
                        "geoserver.backend.pgconfig.cache.enabled=false",
                        "geoserver.security.layergroup-containmentcache=true")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(PgconfigCatalogFacade.class)
                        .hasSingleBean(PgconfigConfigRepository.class)
                        .doesNotHaveBean(EntityCache.class)
                        .doesNotHaveBean(EntityCodec.class)
                        .doesNotHaveBean(EntityCacheMetrics.class)
                        .doesNotHaveBean(EntityCacheEventListener.class)
                        .doesNotHaveBean(EntityCacheLifecycle.class)
                        .doesNotHaveBean(PgconfigLayerGroupWarmUp.class)
                        .doesNotHaveBean("pgconfigEntityCacheBusListener"));
    }

    @Test
    void resourceCacheIsWired() {
        runner.run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(ResourceStoreCache.class)
                    .hasBean("pgconfigResourceStoreCacheMetrics")
                    .hasBean("pgconfigResourceStoreCacheBusListener")
                    .hasSingleBean(ResourceChangeListener.class);
            ResourceStoreCache cache = context.getBean(ResourceStoreCache.class);
            PgconfigResourceStore store = context.getBean("resourceStoreImpl", PgconfigResourceStore.class);

            store.get("styles");

            assertThat(cache.name()).isEqualTo("gs-resources");
            assertThat(cache.size()).isPositive();
        });
    }

    @Test
    void busSubscriptionClearsIndex() {
        runner.run(context -> {
            ResourceStoreCache cache = context.getBean(ResourceStoreCache.class);
            context.getBean("resourceStoreImpl", PgconfigResourceStore.class).get("styles");

            context.publishEvent(new RemoteEventsResumed("geoserver.anonymous.queue"));

            assertThat(cache.size()).isZero();
        });
    }

    // the listener refreshes the index through the transactional proxy of the store, without clearing it
    @Test
    void remoteResourceRefreshesIndex() {
        runner.run(context -> {
            ResourceStoreCache cache = context.getBean(ResourceStoreCache.class);
            context.getBean("resourceStoreImpl", PgconfigResourceStore.class).get("styles");
            String path = "%s.sld".formatted(UUID.randomUUID());
            insertBehindStore(context.getBean("pcconfigJdbcTemplate", JdbcTemplate.class), path);
            ResourceChanged created = ResourceChanged.createLocal(path, Kind.CREATED, Type.RESOURCE, 0L, List.of());
            created.setRemote(true);

            context.publishEvent(created);

            assertThat(cache.size()).isPositive();
            assertThat(cache.find(path)).isPresent();
        });
    }

    private void insertBehindStore(JdbcTemplate template, String path) {
        String sql =
                """
                INSERT INTO resourcestore (parentid, path, "type", content)
                VALUES (0, ?, 'RESOURCE', ?)
                """;
        template.update(sql, path, "<sld/>".getBytes(UTF_8));
    }

    @Test
    void disabledResourceCacheHasNoBean() {
        runner.withPropertyValues("geoserver.backend.pgconfig.cache.resources.enabled=false")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasBean("resourceStoreImpl")
                        .hasBean("pgconfigCatalogEntityCache")
                        .doesNotHaveBean(ResourceStoreCache.class)
                        .doesNotHaveBean("pgconfigResourceStoreCacheMetrics")
                        .doesNotHaveBean("pgconfigResourceStoreCacheBusListener"));
    }

    // the listener tells the listeners of this instance about remote changes, with or without the cache
    @Test
    void disabledResourceCacheKeepsListener() {
        runner.withPropertyValues("geoserver.backend.pgconfig.cache.resources.enabled=false")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(ResourceChangeListener.class));
    }

    // the switch reads 1 as true, as the switch of the request cache does
    @Test
    void resourceCacheEnabledByOne() {
        runner.withPropertyValues("geoserver.backend.pgconfig.cache.resources.enabled=1")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(ResourceStoreCache.class)
                        .hasBean("pgconfigResourceStoreCacheBusListener"));
    }

    // micrometer is an optional dependency of the module
    @Test
    void startsWithoutMicrometer() {
        runner.withClassLoader(new FilteredClassLoader(MeterRegistry.class)).run(context -> assertThat(context)
                .hasNotFailed()
                .hasBean("pgconfigCatalogEntityCache")
                .hasBean("pgconfigConfigEntityCache")
                .hasSingleBean(RequestCacheThreadLocalTransfer.class)
                .doesNotHaveBean(EntityCacheMetrics.class)
                .doesNotHaveBean(RequestCacheMetrics.class));
    }
}
