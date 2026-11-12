/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.config.catalog.backend.pgconfig;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.catalog.plugin.locking.LockProviderGeoServerConfigurationLock;
import org.geoserver.catalog.plugin.requestcache.RemoteChangeCounter;
import org.geoserver.catalog.plugin.requestcache.RequestCacheBudget;
import org.geoserver.catalog.plugin.requestcache.RequestCacheThreadLocalTransfer;
import org.geoserver.catalog.plugin.requestcache.RequestCachingGeoServerFacade;
import org.geoserver.catalog.plugin.resolving.ResolvingProxyResolver;
import org.geoserver.cloud.backend.pgconfig.PgconfigBackendBuilder;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCache;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheBusListener;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheEventListener;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheLifecycle;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheMetrics;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCacheProperties;
import org.geoserver.cloud.backend.pgconfig.cache.EntityCodec;
import org.geoserver.cloud.backend.pgconfig.cache.NoopCodec;
import org.geoserver.cloud.backend.pgconfig.cache.RequestCacheEventListener;
import org.geoserver.cloud.backend.pgconfig.cache.ZstdCodec;
import org.geoserver.cloud.backend.pgconfig.catalog.PgconfigCatalogFacade;
import org.geoserver.cloud.backend.pgconfig.catalog.PgconfigLayerGroupWarmUp;
import org.geoserver.cloud.backend.pgconfig.catalog.repository.PgconfigLayerGroupRepository;
import org.geoserver.cloud.backend.pgconfig.config.PgconfigConfigRepository;
import org.geoserver.cloud.backend.pgconfig.config.PgconfigGeoServerFacade;
import org.geoserver.cloud.backend.pgconfig.config.PgconfigUpdateSequence;
import org.geoserver.cloud.backend.pgconfig.coverage.MosaicSyncingResourcePool;
import org.geoserver.cloud.backend.pgconfig.resource.DbBackedFileSynchronizer;
import org.geoserver.cloud.backend.pgconfig.resource.FileSystemResourceStoreCache;
import org.geoserver.cloud.backend.pgconfig.resource.PgconfigLockProvider;
import org.geoserver.cloud.backend.pgconfig.resource.PgconfigResourceStore;
import org.geoserver.cloud.config.catalog.backend.core.GeoServerBackendConfigurer;
import org.geoserver.cloud.config.catalog.backend.core.RawCatalogCustomizer;
import org.geoserver.cloud.config.catalog.backend.pgconfig.DatabaseMigrationConfiguration.Migrations;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.platform.resource.LockProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

/**
 * Spring configuration for GeoServer Cloud's PostgreSQL-based catalog and configuration backend.
 *
 * <h2>Overview</h2>
 *
 * <p>This configuration provides a fully database-backed GeoServer implementation that stores all catalog,
 * configuration, and resource data in PostgreSQL. Unlike the traditional data directory backend, this implementation
 * enables true cloud-native deployments with:
 *
 * <ul>
 *   <li>Stateless application servers (no local filesystem dependencies)
 *   <li>Horizontal scalability without shared filesystem requirements
 *   <li>ACID guarantees for catalog and configuration changes
 *   <li>Centralized resource storage with file system cache for performance
 * </ul>
 *
 * <h2>Storage Architecture</h2>
 *
 * <ul>
 *   <li><b>Catalog</b> - PostgreSQL-backed catalog facade ({@link PgconfigCatalogFacade}), with a cache of the objects
 *       read by the facade ({@link EntityCache}, {@code gs-catalog}). The services running the layer group containment
 *       cache load the layer groups into the cache ahead of their lookups ({@link PgconfigLayerGroupWarmUp}).
 *   <li><b>Configuration</b> - PostgreSQL repository ({@link PgconfigConfigRepository},
 *       {@link PgconfigGeoServerFacade}), with a cache of its own ({@code gs-config}). The caches evict the objects
 *       changed by other service instances ({@link EntityCacheEventListener}), and are suspended while GeoServer
 *       reloads ({@link EntityCacheLifecycle}).
 *   <li><b>Request cache</b> - the catalog and configuration objects looked up by a request, kept until it ends
 *       ({@link RequestCacheFilter}): the request decodes each object once, and resolves the references of a catalog
 *       object once. The caches of the requests in flight share one budget of entries ({@link RequestCacheBudget}).
 *   <li><b>Resources</b> - Hybrid storage with PostgreSQL backend and local filesystem cache
 *       ({@link PgconfigResourceStore})
 *   <li><b>Locking</b> - Database-backed distributed locks via Spring Integration JDBC locks ({@link JdbcLockRegistry})
 *   <li><b>Update Sequence</b> - Database-tracked version for WMS/WFS capabilities caching
 *       ({@link PgconfigUpdateSequence})
 * </ul>
 *
 * <h2>Distributed Locking</h2>
 *
 * <p>Provides cluster-wide coordination through {@link DefaultLockRepository} with:
 *
 * <ul>
 *   <li>Instance-specific lock identification via {@code info.instance-id} property
 *   <li>Custom table prefix {@code RESOURCE_} (instead of default {@code INT_})
 *   <li>300-second lock timeout for dead lock recovery
 *   <li>Explicit initialization to ensure transaction template creation in all Spring contexts
 * </ul>
 *
 * <h2>Database Schema</h2>
 *
 * <p>Requires database schema initialization via {@link DatabaseMigrationConfiguration}, which this configuration
 * depends on through constructor injection. Migrations create tables for:
 *
 * <ul>
 *   <li>Catalog entities (workspaces, namespaces, stores, layers, styles, etc.)
 *   <li>Configuration objects (services, settings, logging)
 *   <li>Resource storage (binary content with metadata)
 *   <li>Distributed locks ({@code RESOURCE_LOCK} table)
 * </ul>
 *
 * <h2>Configuration</h2>
 *
 * <p>Backend behavior is controlled through configuration properties:
 *
 * <pre>
 * geoserver.backend.pgconfig.enabled=true
 * geoserver.backend.pgconfig.datasource.url=jdbc:postgresql://host:5432/geoserver
 * geoserver.backend.pgconfig.datasource.username=geoserver
 * geoserver.backend.pgconfig.datasource.password=secret
 * info.instance-id=${HOSTNAME:}  # For distributed locking identifier
 * </pre>
 *
 * <h2>Bean Dependencies</h2>
 *
 * <p>This configuration uses {@code @Configuration(proxyBeanMethods = false)} for optimal performance and flexibility,
 * allowing bean methods to declare their dependencies as method parameters rather than calling other {@code @Bean}
 * methods directly. This enables proper dependency injection and avoids issues with CGLIB proxies.
 *
 * @since 1.4
 * @see GeoServerBackendConfigurer
 * @see DatabaseMigrationConfiguration
 * @see PgconfigBackendBuilder
 * @see PgconfigResourceStore
 * @see JdbcLockRegistry
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({PgconfigBackendProperties.class, EntityCacheProperties.class})
@Slf4j(topic = "org.geoserver.cloud.config.catalog.backend.pgconfig")
public class PgconfigBackendConfiguration implements GeoServerBackendConfigurer {

    private static final long CONFIG_CACHE_MAXIMUM_WEIGHT = 16L * 1024 * 1024;

    /**
     * The order of the filter opening the cache of a request: before the filter of Spring Security at -100 and the
     * GeoServer filters from 1 to 5, as their security checks look catalog objects up.
     */
    private static final int REQUEST_CACHE_FILTER_ORDER = -1000;

    /**
     * Constructs the PostgreSQL backend configuration.
     *
     * <p>The {@link Migrations} parameter ensures that database schema initialization has completed before this
     * configuration creates any beans that depend on database tables.
     *
     * @param migrations database migration tracker that confirms schema is ready
     */
    PgconfigBackendConfiguration(Migrations migrations) {
        log.info(
                "Loading geoserver config backend with {}. {}",
                PgconfigBackendConfiguration.class.getSimpleName(),
                migrations);
    }

    /**
     * The catalog facade on the database, with the catalog cache unless
     * {@code geoserver.backend.pgconfig.cache.enabled} is false. Its objects hold their references as proxies, resolved
     * by the {@code catalogFacade} bean.
     */
    @Bean
    PgconfigCatalogFacade pgconfigCatalogFacade(
            @Qualifier("pgconfigDataSource") DataSource dataSource,
            @Qualifier("pgconfigCatalogEntityCache") ObjectProvider<EntityCache> catalogCache,
            EntityCacheProperties cacheProperties) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        PgconfigCatalogFacade facade = new PgconfigCatalogFacade(template, catalogCache.getIfAvailable());
        facade.setListingWriteThrough(cacheProperties.isListingWriteThrough());
        facade.setRequestCacheMaxEntries(cacheProperties.getRequest().getMaxEntries());
        return facade;
    }

    /**
     * The facade given to the catalog: the cache of the request, over a decorator resolving the references of the
     * objects below the workspace isolation added by the catalog, over the pgconfig facade. The request keeps the
     * objects resolved, and the decorator and the repositories look the references up through the cache of the request,
     * see {@link PgconfigBackendBuilder#requestCaching}.
     */
    @Bean("catalogFacade")
    @ConditionalOnPgconfigRequestCacheEnabled
    ExtendedCatalogFacade pgconfigRequestCachingCatalogFacade(
            @Qualifier("pgconfigCatalogFacade") PgconfigCatalogFacade facade) {
        return PgconfigBackendBuilder.requestCaching(facade);
    }

    /**
     * The facade given to the catalog if {@code geoserver.backend.pgconfig.cache.request.enabled} is false: the
     * decorator of {@link PgconfigBackendBuilder#resolving}, directly over the pgconfig facade.
     */
    @Bean("catalogFacade")
    @ConditionalOnPgconfigRequestCacheDisabled
    ExtendedCatalogFacade pgconfigResolvingCatalogFacade(
            @Qualifier("pgconfigCatalogFacade") PgconfigCatalogFacade facade) {
        return PgconfigBackendBuilder.resolving(facade);
    }

    /**
     * The cache of the catalog objects read by the repositories, named {@code gs-catalog} in the metrics. Its size is
     * bounded by a share of the heap.
     */
    @Bean
    @ConditionalOnPgconfigCacheEnabled
    EntityCache pgconfigCatalogEntityCache(
            EntityCacheProperties cacheProperties, @Qualifier("pgconfigCatalogCacheCodec") EntityCodec codec) {
        long maximumWeight = EntityCache.maximumWeightFor(cacheProperties.getMaxHeapPercent());
        return new EntityCache("gs-catalog", maximumWeight, cacheProperties.getMissTtl(), codec);
    }

    /**
     * The cache of the configuration objects read by the configuration repository, named {@code gs-config} in the
     * metrics. The configuration objects have a cache of their own: the catalog objects cannot push them out. They are
     * few: their cache takes 1% of the heap, 16 MiB at most.
     */
    @Bean
    @ConditionalOnPgconfigCacheEnabled
    EntityCache pgconfigConfigEntityCache(
            EntityCacheProperties cacheProperties, @Qualifier("pgconfigCatalogCacheCodec") EntityCodec codec) {
        long maximumWeight = configCacheMaximumWeight(EntityCache.maximumWeightFor(1));
        return new EntityCache("gs-config", maximumWeight, cacheProperties.getMissTtl(), codec);
    }

    /** The maximum weight of the configuration cache: a share of the heap, and 16 MiB at most. */
    static long configCacheMaximumWeight(long heapShare) {
        return Math.min(heapShare, CONFIG_CACHE_MAXIMUM_WEIGHT);
    }

    /**
     * The codec of the cached objects, chosen by {@code geoserver.backend.pgconfig.cache.compression}. Spring closes
     * the zstd codec at shutdown to free its native memory.
     */
    @Bean
    @ConditionalOnPgconfigCacheEnabled
    EntityCodec pgconfigCatalogCacheCodec(EntityCacheProperties cacheProperties) {
        return switch (cacheProperties.getCompression()) {
            case NONE -> new NoopCodec();
            case ZSTD -> zstdOrUncompressed(() -> ZstdCodec.withBundledDictionary(ZstdCodec.DEFAULT_LEVEL));
        };
    }

    /**
     * Returns the zstd codec, or no compression if zstd does not load: the backend must start on a platform without the
     * native library of zstd.
     */
    static EntityCodec zstdOrUncompressed(Supplier<EntityCodec> zstd) {
        try {
            return zstd.get();
        } catch (LinkageError | IllegalStateException e) {
            log.warn(
                    "geoserver.backend.pgconfig.cache.compression is zstd, but zstd is not available. The cache keeps the objects uncompressed. {}",
                    e.toString());
            return new NoopCodec();
        }
    }

    /**
     * Registers the metrics of the caches. A nested class keeps the Micrometer types out of the methods of the outer
     * configuration: Spring reads those methods, and fails on a type missing from the classpath.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnPgconfigCacheEnabled
    static class EntityCacheMetricsConfiguration {

        @Bean
        EntityCacheMetrics pgconfigCatalogEntityCacheMetrics(
                @Qualifier("pgconfigCatalogEntityCache") EntityCache catalogCache) {
            return new EntityCacheMetrics(catalogCache);
        }

        @Bean
        EntityCacheMetrics pgconfigConfigEntityCacheMetrics(
                @Qualifier("pgconfigConfigEntityCache") EntityCache configCache) {
            return new EntityCacheMetrics(configCache);
        }
    }

    /** Evicts the entries of the caches made stale by the changes of other service instances. */
    @Bean
    @ConditionalOnPgconfigCacheEnabled
    EntityCacheEventListener pgconfigEntityCacheEventListener(List<EntityCache> caches) {
        return new EntityCacheEventListener(caches);
    }

    /**
     * Suspends the caches while GeoServer reloads its configuration, and resumes them once it is loaded. If the service
     * runs the layer group containment cache, the layer groups are loaded again in the background once the caches serve
     * again after a reset or a reload.
     */
    @Bean
    @ConditionalOnPgconfigCacheEnabled
    EntityCacheLifecycle pgconfigEntityCacheLifecycle(
            List<EntityCache> caches, ObjectProvider<PgconfigLayerGroupWarmUp> groupWarmUp) {
        EntityCacheLifecycle lifecycle = new EntityCacheLifecycle(caches);
        groupWarmUp.ifAvailable(warmUp -> lifecycle.setAfterServingAgain(warmUp::warmUpInBackground));
        return lifecycle;
    }

    /**
     * Loads the layer groups and their members into the catalog cache, ahead of the lookups of the access checks. The
     * layer group containment cache runs it before each build, as a {@code LayerGroupWarmUp} bean.
     *
     * <p>Its property condition is a copy of the condition of the containment cache bean of
     * {@code CoreBackendConfiguration}, and must stay equal to it. The services without the containment cache, such as
     * WFS, do not look up the groups containing a layer, and do not load the groups.
     */
    @Bean
    @ConditionalOnPgconfigCacheEnabled
    @ConditionalOnProperty(
            name = "geoserver.security.layergroup-containmentcache",
            havingValue = "true",
            matchIfMissing = false)
    PgconfigLayerGroupWarmUp pgconfigLayerGroupWarmUp(
            @Qualifier("pgconfigCatalogFacade") PgconfigCatalogFacade facade,
            @Qualifier("pgconfigCatalogEntityCache") EntityCache catalogCache) {
        PgconfigLayerGroupRepository groups = facade.repository(LayerGroupInfo.class);
        return new PgconfigLayerGroupWarmUp(groups, catalogCache);
    }

    /** Empties the entity caches when the events of the other service instances reach this one again. */
    @Bean
    @ConditionalOnPgconfigCacheEnabled
    EntityCacheBusListener pgconfigEntityCacheBusListener(List<EntityCache> caches) {
        return new EntityCacheBusListener(caches);
    }

    /**
     * Opens the cache of each request, before the filters of Spring Security and GeoServer: their security checks look
     * catalog objects up. A forward or an include runs within the scope of its request.
     */
    @Bean
    @ConditionalOnPgconfigRequestCacheEnabled
    FilterRegistrationBean<RequestCacheFilter> pgconfigRequestCacheFilter(EntityCacheProperties cacheProperties) {
        int maxEntries = cacheProperties.getRequest().getMaxEntries();
        RequestCacheFilter filter = new RequestCacheFilter(maxEntries);
        FilterRegistrationBean<RequestCacheFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setName("pgconfigRequestCacheFilter");
        registration.addUrlPatterns("/*");
        registration.setOrder(REQUEST_CACHE_FILTER_ORDER);
        return registration;
    }

    /**
     * Sets the budget of entries shared by the request caches of the JVM. The bean is the budget of the JVM, see
     * {@link RequestCacheBudget#shared()}: the caches are opened by static scopes, outside the application context.
     */
    @Bean
    @ConditionalOnPgconfigRequestCacheEnabled
    RequestCacheBudget pgconfigRequestCacheBudget(EntityCacheProperties cacheProperties) {
        RequestCacheBudget budget = RequestCacheBudget.shared();
        budget.setMaxTotalEntries(cacheProperties.getRequest().getMaxTotalEntries());
        return budget;
    }

    /**
     * Tells the request caches in flight about the changes made through other service instances. The caches follow
     * {@link RemoteChangeCounter#shared()}, as they share the budget of the JVM.
     */
    @Bean
    @ConditionalOnPgconfigRequestCacheEnabled
    RequestCacheEventListener pgconfigRequestCacheEventListener() {
        return new RequestCacheEventListener(RemoteChangeCounter.shared());
    }

    /**
     * Hands the cache of a request to the worker threads of the request, such as the WMS rendering threads. GeoServer
     * collects the transfers from the beans of the application context.
     */
    @Bean
    @ConditionalOnPgconfigRequestCacheEnabled
    RequestCacheThreadLocalTransfer pgconfigRequestCacheTransfer() {
        return new RequestCacheThreadLocalTransfer();
    }

    /**
     * Registers the counts of the request caches. A nested class keeps the Micrometer types out of the methods of the
     * outer configuration, as for the entity caches.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnPgconfigRequestCacheEnabled
    static class RequestCacheMetricsConfiguration {

        @Bean
        RequestCacheMetrics pgconfigRequestCacheMetrics() {
            return new RequestCacheMetrics();
        }
    }

    @Bean(name = "pcconfigJdbcTemplate")
    JdbcTemplate pcconfigJdbcTemplate(@Qualifier("pgconfigDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    GeoServerConfigurationLock configurationLock(@Qualifier("pgconfigLockProvider") LockProvider lockProvider) {
        return new LockProviderGeoServerConfigurationLock(lockProvider);
    }

    /**
     * The update sequence, on the configuration facade without the cache of the request. Its transaction must read the
     * global settings from the database: a copy read earlier by the request would miss a change made by another service
     * instance, and saving the new value from that copy would undo the change.
     */
    @Primary
    @Bean
    @DependsOnDatabaseInitialization
    PgconfigUpdateSequence updateSequence(
            @Qualifier("pgconfigGeoServerFacade") PgconfigGeoServerFacade geoserverFacade,
            @Qualifier("pgconfigDataSource") DataSource dataSource) {
        return new PgconfigUpdateSequence(dataSource, geoserverFacade);
    }

    @Bean
    GeoServerLoader geoServerLoaderImpl(
            PgconfigGeoServerResourceLoader resourceLoader, GeoServerConfigurationLock configurationLock) {
        log.debug("Creating GeoServerLoader {}", PgconfigGeoServerLoader.class.getSimpleName());
        return new PgconfigGeoServerLoader(resourceLoader, configurationLock);
    }

    /**
     * The configuration repository, with the configuration cache unless
     * {@code geoserver.backend.pgconfig.cache.enabled} is false.
     */
    @Bean
    PgconfigConfigRepository configRepository(
            @Qualifier("pcconfigJdbcTemplate") JdbcTemplate template,
            @Qualifier("pgconfigConfigEntityCache") ObjectProvider<EntityCache> configCache) {
        PgconfigConfigRepository repository = new PgconfigConfigRepository(template);
        repository.setEntityCache(configCache.getIfAvailable());
        return repository;
    }

    /**
     * The configuration facade on the database. It looks the workspaces of settings and services up in the pgconfig
     * catalog facade, like the references of the catalog objects.
     */
    @Bean
    PgconfigGeoServerFacade pgconfigGeoServerFacade(
            PgconfigConfigRepository configRepository,
            @Qualifier("pgconfigCatalogFacade") PgconfigCatalogFacade catalogFacade) {
        PgconfigGeoServerFacade facade = new PgconfigGeoServerFacade(configRepository);
        facade.setWorkspaceResolver(ResolvingProxyResolver.of(catalogFacade));
        return facade;
    }

    /** The configuration facade given to GeoServer: the cache of the request, over the pgconfig facade. */
    @Bean("geoserverFacade")
    @ConditionalOnPgconfigRequestCacheEnabled
    GeoServerFacade pgconfigRequestCachingGeoServerFacade(
            @Qualifier("pgconfigGeoServerFacade") PgconfigGeoServerFacade facade) {
        return new RequestCachingGeoServerFacade(facade);
    }

    /**
     * The configuration facade given to GeoServer if {@code geoserver.backend.pgconfig.cache.request.enabled} is false:
     * the pgconfig facade itself, under a second bean name.
     */
    @Bean("geoserverFacade")
    @ConditionalOnPgconfigRequestCacheDisabled
    GeoServerFacade pgconfigGeoServerFacadeWithoutRequestCache(
            @Qualifier("pgconfigGeoServerFacade") PgconfigGeoServerFacade facade) {
        return facade;
    }

    /**
     * Creates the {@link PgconfigResourceStore} bean.
     *
     * <p>The return type is the concrete class rather than the {@link org.geoserver.platform.resource.ResourceStore
     * ResourceStore} interface because {@link PgconfigGeoServerResourceLoader} needs access to
     * {@link PgconfigResourceStore#getLockProvider()}, which is not part of the {@code ResourceStore} contract. This
     * works because
     * {@link org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.PgconfigTransactionManagerAutoConfiguration
     * PgconfigTransactionManagerAutoConfiguration} enables CGLIB proxying (via
     * {@code @EnableTransactionManagement(proxyTargetClass = true)}), ensuring the transaction proxy preserves the
     * concrete type.
     *
     * @see PgconfigGeoServerResourceLoader#getLockProvider()
     */
    @Bean
    PgconfigResourceStore resourceStoreImpl(
            @Qualifier("pgconfigLockProvider") LockProvider lockProvider,
            FileSystemResourceStoreCache resourceStoreCache,
            @Qualifier("pcconfigJdbcTemplate") JdbcTemplate template,
            PgconfigBackendProperties configProperties) {

        log.debug("Creating ResourceStore {}", PgconfigResourceStore.class.getSimpleName());
        Predicate<String> localOnlyFilter = PgconfigResourceStore.defaultIgnoredResources();
        Predicate<String> dbBackedFilePatterns =
                PgconfigResourceStore.antPathMatcher(configProperties.getDbBackedFilePatterns());
        return new PgconfigResourceStore(
                resourceStoreCache,
                template,
                (PgconfigLockProvider) lockProvider,
                localOnlyFilter,
                dbBackedFilePatterns);
    }

    @Bean
    FileSystemResourceStoreCache pgconfigFileSystemResourceStoreCache(PgconfigBackendProperties configProperties) {
        Predicate<String> noClobber = PgconfigResourceStore.antPathMatcher(configProperties.getDbBackedFilePatterns());
        return FileSystemResourceStoreCache.newTempDirInstance(noClobber);
    }

    /**
     * Keeps db-backed files written with raw file I/O (e.g. ImageMosaic config files rewritten by gt-imagemosaic)
     * consistent between each pod's local resource cache and the resource store, making them visible to every pod.
     *
     * @see MosaicSyncingResourcePool
     */
    @Bean
    DbBackedFileSynchronizer pgconfigDbBackedFileSynchronizer(
            @Qualifier("resourceStoreImpl") PgconfigResourceStore resourceStore,
            FileSystemResourceStoreCache resourceStoreCache) {
        return new DbBackedFileSynchronizer(resourceStore, resourceStoreCache);
    }

    /**
     * Installs {@link MosaicSyncingResourcePool} on the raw catalog, making REST-created ImageMosaics work across pods
     * on the pgconfig backend.
     */
    @Bean
    RawCatalogCustomizer pgconfigMosaicSyncCustomizer(DbBackedFileSynchronizer synchronizer) {
        return rawCatalog -> rawCatalog.setResourcePool(new MosaicSyncingResourcePool(rawCatalog, synchronizer));
    }

    /**
     * Creates the {@link PgconfigGeoServerResourceLoader} bean.
     *
     * <p>Injects the concrete {@link PgconfigResourceStore} (not the
     * {@link org.geoserver.platform.resource.ResourceStore ResourceStore} interface) because
     * {@link PgconfigGeoServerResourceLoader} needs {@link PgconfigResourceStore#getLockProvider()} to provide the lock
     * provider to {@link PgconfigGeoServerLoader}.
     *
     * @see #resourceStoreImpl
     */
    @Bean
    PgconfigGeoServerResourceLoader resourceLoader(
            @Qualifier("resourceStoreImpl") PgconfigResourceStore resourceStore) {
        log.debug("Creating GeoServerResourceLoader {}", PgconfigGeoServerResourceLoader.class.getSimpleName());
        return new PgconfigGeoServerResourceLoader(resourceStore);
    }

    @Bean
    LockProvider pgconfigLockProvider(@Qualifier("pgconfigLockRegistry") JdbcLockRegistry pgconfigLockRegistry) {
        log.debug("Creating {}", PgconfigLockProvider.class.getSimpleName());
        return new PgconfigLockProvider(pgconfigLockRegistry);
    }

    @Bean
    JdbcLockRegistry pgconfigLockRegistry(@Qualifier("pgconfigLockRepository") LockRepository pgconfigLockRepository) {
        log.debug("Creating {}", LockRegistry.class.getSimpleName());
        Duration ttl = Duration.ofSeconds(30);
        return new JdbcLockRegistry(pgconfigLockRepository, ttl);
    }

    @Bean
    LockRepository pgconfigLockRepository(
            @Qualifier("pgconfigDataSource") DataSource dataSource,
            @Value("${info.instance-id:}") String instanceId,
            @Qualifier("pgconfigTransactionManager") PlatformTransactionManager pgconfigTransactionManager) {

        log.debug("Creating {} for instance {}", LockRepository.class.getSimpleName(), instanceId);
        DefaultLockRepository lockRepository;
        if (StringUtils.hasLength(instanceId)) {
            lockRepository = new DefaultLockRepository(dataSource, instanceId);
        } else {
            lockRepository = new DefaultLockRepository(dataSource);
        }
        lockRepository.setTransactionManager(pgconfigTransactionManager);
        // override default table prefix "INT" by "RESOURCE_" (matching table definition
        // RESOURCE_LOCK in init.XXX.sql
        lockRepository.setPrefix("RESOURCE_");
        // Explicitly initialize the lock repository to ensure transaction template is created
        lockRepository.afterSingletonsInstantiated();
        return lockRepository;
    }
}
