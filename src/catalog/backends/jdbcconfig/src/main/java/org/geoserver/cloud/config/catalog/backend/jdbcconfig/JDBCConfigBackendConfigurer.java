/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.jdbcconfig;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zaxxer.hikari.HikariDataSource;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.plugin.CatalogFacadeExtensionAdapter;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.cloud.config.catalog.backend.core.GeoServerBackendConfigurer;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.jdbcconfig.JDBCGeoServerLoader;
import org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.jdbcconfig.internal.JDBCConfigProperties;
import org.geoserver.jdbcconfig.internal.JDBCConfigXStreamPersisterInitializer;
import org.geoserver.jdbcconfig.internal.XStreamInfoSerialBinding;
import org.geoserver.jdbcstore.JDBCResourceStore;
import org.geoserver.jdbcstore.cache.ResourceCache;
import org.geoserver.jdbcstore.cache.SimpleResourceCache;
import org.geoserver.jdbcstore.internal.JDBCQueryHelper;
import org.geoserver.jdbcstore.locks.LockRegistryAdapter;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.SimpleResourceNotificationDispatcher;
import org.geoserver.util.CacheProvider;
import org.geoserver.util.DefaultCacheProvider;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.DatabaseStartupValidator;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.sql.Driver;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

/**
 * Configure JDBC Config catalog
 *
 * <p>Configuration properties:
 *
 * <ul>
 *   <li>{@code geoserver.backend.jdbcconfig.enabled}: whether to enable jdbc-config, provided it's
 *       in the classpath. Defaults to {@code true}
 *   <li>{@code geoserver.backend.jdbcconfig.web.enabled}: whether to enable jdbc-config wicket
 *       components, provided {@code jdbcconfig} is enabled and the geoserver web-ui is in the
 *       classpath. Defaults to {@code true}.
 *   <li>{@code geoserver.backend.jdbcconfig.datasource.url}: connection URL (e.g. {@code
 *       jdbc:postgresql://localhost:5432/gsconfigdb})
 *   <li>{@code geoserver.backend.jdbcconfig.datasource.username}:
 *   <li>{@code geoserver.backend.jdbcconfig.datasource.password}:
 *   <li>{@code geoserver.backend.jdbcconfig.datasource.driverClassname}: JDBC {@link Driver} class
 *       name (e.g. {@code org.postgresql.Driver})
 * </ul>
 *
 * The {@link DataSource} is provided using regular spring-boot data source configuration properties
 * using the {@code geoserver.jbcconfig.datasource} property name prefix.
 *
 * <p>The following properties are the minimum necessary to configure the data source:
 *
 * <pre>{@code
 * geoserver.backend.jdbcconfig.datasource.jdbc-url
 * geoserver.backend.jdbcconfig.datasource.driver-class-name
 * geoserver.backend.jdbcconfig.datasource.username
 * geoserver.backend.jdbcconfig.datasource.password
 * }</pre>
 *
 * Additionally, you can further configure the connection pool using any of the <a
 * href="https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby">Hikari</a> connection
 * pool property names, for example:
 *
 * <pre>{@code
 * geoserver.backend.jdbcconfig.datasource.schema=public
 * geoserver.backend.jdbcconfig.datasource.minimumIdle=2
 * geoserver.backend.jdbcconfig.datasource.maximumPoolSize=10
 * geoserver.backend.jdbcconfig.datasource.connectionTimeout=250
 * geoserver.backend.jdbcconfig.datasource.idletTimeout=60000
 * }</pre>
 */
@Configuration(proxyBeanMethods = true)
@EnableTransactionManagement
@EnableConfigurationProperties(JdbcConfigConfigurationProperties.class)
@Slf4j(topic = "org.geoserver.cloud.config.jdbcconfig")
public class JDBCConfigBackendConfigurer extends GeoServerBackendConfigurer {

    private String instanceId;
    private JdbcConfigConfigurationProperties jdbcconfigConfig;

    public JDBCConfigBackendConfigurer(
            JdbcConfigConfigurationProperties backendConfig,
            @Value("${info.instance-id:}") String instanceId) {
        this.jdbcconfigConfig = backendConfig;
        this.instanceId = instanceId;
        log.info(
                "Loading geoserver config backend with {}",
                JDBCConfigBackendConfigurer.class.getSimpleName());
    }

    @DependsOn("jdbcConfigDataSourceStartupValidator")
    protected @Bean @Override UpdateSequence updateSequence() {
        DataSource dataSource = jdbcConfigDataSource();
        CloudJdbcConfigProperties props = jdbcConfigProperties();
        GeoServerFacade geoserverFacade = geoserverFacade();
        ConfigDatabase db = jdbcConfigDB();
        return new JdbcConfigUpdateSequence(dataSource, props, geoserverFacade, db);
    }

    protected @Bean @Override GeoServerConfigurationLock configurationLock() {
        return new GeoServerConfigurationLock();
    }

    @DependsOn("jdbcConfigDataSourceStartupValidator")
    @ConfigurationProperties(prefix = "geoserver.backend.jdbcconfig")
    @Bean
    CloudJdbcConfigProperties jdbcConfigProperties() {
        DataSource dataSource = jdbcConfigDataSource();
        CloudJdbcConfigProperties props = new CloudJdbcConfigProperties(dataSource);

        // dataSourceId shows up in geoserver's home page so set it here
        JdbcConfigConfigurationProperties backendProperties = this.jdbcconfigConfig;
        DataSourceProperties dataSourceProperties = backendProperties.getDatasource();
        String jdbcUrl = dataSourceProperties.getUrl();
        props.setDatasourceId(jdbcUrl);
        return props;
    }

    @DependsOn("jdbcConfigDataSourceStartupValidator")
    @ConfigurationProperties(prefix = "geoserver.backend.jdbcconfig")
    @Bean
    CloudJdbcStoreProperties jdbcStoreProperties() {
        return new CloudJdbcStoreProperties(jdbcConfigDataSource());
    }

    @DependsOn({"extensions", "jdbcConfigDataSourceStartupValidator"})
    protected @Bean @Override GeoServerResourceLoader resourceLoader() {
        JdbcConfigConfigurationProperties configProperties = this.jdbcconfigConfig;
        Path path = configProperties.getCacheDirectory();
        File dataDirectory = path == null ? null : path.toFile();
        GeoServerResourceLoader loader = new GeoServerResourceLoader(resourceStoreImpl());
        loader.setBaseDirectory(dataDirectory);
        return loader;
    }

    @DependsOn({
        "extensions",
        "JDBCConfigDB",
        "jdbcConfigDataSourceStartupValidator",
        "noopCacheProvider"
    })
    @Bean(name = {"resourceStoreImpl"})
    protected @Override ResourceStore resourceStoreImpl() {

        System.setProperty(DefaultCacheProvider.BEAN_NAME_PROPERTY, "noopCacheProvider");

        final JDBCConfigProperties jdbcConfigProperties = jdbcConfigProperties();
        final CloudJdbcStoreProperties jdbcStoreProperties = jdbcStoreProperties();
        final ConfigDatabase jdbcConfigDB = jdbcConfigDB();

        initDbSchema(jdbcConfigProperties, jdbcStoreProperties, jdbcConfigDB);
        // no fall back to data directory, jdbcconfig is either enabled and fully engaged, or
        // not at all
        JDBCResourceStore resourceStore;
        resourceStore = new JDBCResourceStore(jdbcConfigDataSource(), jdbcStoreProperties);
        resourceStore.setCache(jdbcResourceCache());
        resourceStore.setLockProvider(jdbcStoreLockProvider());
        resourceStore.setResourceNotificationDispatcher(resourceNotificationDispatcher());
        return resourceStore;
    }

    @DependsOn({"extensions", "jdbcConfigDataSourceStartupValidator"})
    @Bean
    LockRegistryAdapter jdbcStoreLockProvider() {
        return new LockRegistryAdapter(jdbcLockRegistry());
    }

    @DependsOn("jdbcConfigDataSourceStartupValidator")
    @Bean
    JdbcLockRegistry jdbcLockRegistry() {
        return new JdbcLockRegistry(jdbcLockRepository());
    }

    @DependsOn("jdbcConfigDataSourceStartupValidator")
    @Bean
    DefaultLockRepository jdbcLockRepository() {
        String id = this.instanceId;
        DefaultLockRepository lockRepository;
        if (StringUtils.hasLength(id)) {
            lockRepository = new DefaultLockRepository(jdbcConfigDataSource(), id);
        } else {
            lockRepository = new DefaultLockRepository(jdbcConfigDataSource());
        }
        // override default table prefix "INT" by "RESOURCE_" (matching table definition
        // RESOURCE_LOCK in init.XXX.sql
        lockRepository.setPrefix("RESOURCE_");
        // time in ms to expire dead locks (10k is the default)
        lockRepository.setTimeToLive(300_000);
        return lockRepository;
    }

    @DependsOn("jdbcConfigDataSourceStartupValidator")
    @Bean(name = {"catalogFacade", "JDBCCatalogFacade"})
    protected @Override ExtendedCatalogFacade catalogFacade() {
        ConfigDatabase configDB = jdbcConfigDB();
        JDBCConfigProperties catalogConfig = jdbcConfigProperties();
        CloudJdbcStoreProperties storeConfig = jdbcStoreProperties();
        if (!initDbSchema(catalogConfig, storeConfig, configDB)) {
            try {
                // as ugly as it looks, it expects to be called even with null, in which case it
                // only initializes the dbmappings
                configDB.initDb(null);
            } catch (IOException e) {
                throw new BeanInitializationException(e.getMessage(), e);
            }
        }
        JDBCCatalogFacade legacyFacade = new CloudJdbcCatalogFacade(jdbcConfigDB());
        return new CatalogFacadeExtensionAdapter(legacyFacade);
    }

    @DependsOn("jdbcConfigDataSourceStartupValidator")
    @Bean(name = {"geoserverFacade", "JDBCGeoServerFacade"})
    protected @Override GeoServerFacade geoserverFacade() {
        initDbSchema(jdbcConfigProperties(), jdbcStoreProperties(), jdbcConfigDB());
        ConfigDatabase configDB = jdbcConfigDB();
        return new CloudJdbcGeoserverFacade(configDB);
    }

    @Bean(name = {"geoServerLoaderImpl", "JDBCGeoServerLoader"})
    @DependsOn({
        "catalogFacade",
        "geoserverFacade",
        "extensions",
        "wmsLoader",
        "wfsLoader",
        "wcsLoader",
        "wpsServiceLoader",
        "wmtsLoader"
    })
    protected @Override CloudJdbcGeoServerLoader geoServerLoaderImpl() {
        JDBCConfigProperties config = jdbcConfigProperties();
        ConfigDatabase configdb = jdbcConfigDB();
        Catalog rawCatalog = (Catalog) GeoServerExtensions.bean("rawCatalog");
        GeoServer geoserver = (GeoServer) GeoServerExtensions.bean("geoServer");
        try {
            return new CloudJdbcGeoServerLoader(
                    rawCatalog, geoserver, resourceLoader(), config, configdb);
        } catch (Exception e) {
            throw new BeanInstantiationException(JDBCGeoServerLoader.class, e.getMessage(), e);
        }
    }

    private ResourceCache jdbcResourceCache() {
        CloudJdbcStoreProperties storeProperties = jdbcStoreProperties();
        File baseDirectory = storeProperties.getCacheDirectory();
        return new SimpleResourceCache(baseDirectory);
    }

    @Bean
    XStreamPersisterFactory xstreamPersisterFactory() {
        return new XStreamPersisterFactory();
    }

    @DependsOn({"jdbcConfigDataSourceStartupValidator", "xstreamPersisterFactory"})
    @Bean
    XStreamInfoSerialBinding jdbcPersistenceBinding() {
        return new XStreamInfoSerialBinding(xstreamPersisterFactory());
    }

    @DependsOn({"jdbcConfigDataSourceStartupValidator", "jdbcConfigDataSource"})
    @Bean(name = "JDBCConfigDB")
    ConfigDatabase jdbcConfigDB() {
        CloudJdbcConfigProperties config = jdbcConfigProperties();
        DataSource dataSource = jdbcConfigDataSource();
        XStreamInfoSerialBinding binding = jdbcPersistenceBinding();
        CacheProvider cacheProvider = jdbcCacheProvider();
        return new CloudJdbcConfigDatabase(config, dataSource, binding, cacheProvider);
    }

    private boolean initDbSchema(
            JDBCConfigProperties catalogConfig,
            CloudJdbcStoreProperties resourceStoreConfig,
            ConfigDatabase configDb) {
        // Need to check whether the db needs to be initialized here because secureCatalog's
        // constructor may use the CatalogFacade during initialization (by means of the
        // DefaultResourceAccessManager), before JDBCGeoServerLoader.setCatalogFacade() was called
        try {
            final boolean initCatalogDb = catalogConfig.isInitDb();
            final boolean initStoreDb = resourceStoreConfig.isInitDb();
            if (initCatalogDb) {
                Resource catalogScript = catalogConfig.getInitScript();
                configDb.initDb(catalogScript);
            }
            if (initStoreDb) {
                Resource resourceStoreInitScript = resourceStoreConfig.getInitScript();
                class StoreHelper extends JDBCQueryHelper {
                    public StoreHelper(DataSource ds) {
                        super(ds);
                    }

                    @Override
                    public void runScript(Resource script) {
                        super.runScript(script);
                    }
                }
                new StoreHelper(jdbcConfigDataSource()).runScript(resourceStoreInitScript);
            }
            catalogConfig.setInitDb(false);
            resourceStoreConfig.setInitDb(false);
            return initStoreDb || initCatalogDb;
        } catch (IOException e) {
            throw new BeanInstantiationException(ConfigDatabase.class, e.getMessage(), e);
        }
    }

    // <bean id="JDBCCacheProvider"
    // class="org.geoserver.jdbcconfig.internal.JDBCCacheProvider"/>
    @DependsOn({"extensions"})
    @Bean("JDBCCacheProvider")
    CacheProvider jdbcCacheProvider() {
        return new CloudJdbcConfigCacheProvider();
    }

    @Bean
    CacheProvider noopCacheProvider() {
        return new CacheProvider() {

            @SuppressWarnings("rawtypes")
            private final Cache noOpCache = CacheBuilder.newBuilder().maximumSize(0).build();

            @SuppressWarnings("unchecked")
            @Override
            public <K extends Serializable, V extends Serializable> Cache<K, V> getCache(
                    String cacheName) {
                return noOpCache;
            }
        };
    }

    private static class CloudJdbcConfigCacheProvider implements org.geoserver.util.CacheProvider {

        private final ConcurrentMap<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public <K extends Serializable, V extends Serializable> Cache<K, V> getCache(
                @NonNull String cacheName) {

            return (Cache<K, V>) caches.computeIfAbsent(cacheName, this::newCache);
        }

        protected <K, V> Cache<K, V> newCache(String name) {

            int concurrencyLevel = Runtime.getRuntime().availableProcessors();
            final double maxMiB = Runtime.getRuntime().maxMemory() / 1024d / 1024d;
            final int entriesPerMiB = 50;

            long expirationMinutes = 10;
            long maxEntries = (long) (maxMiB * entriesPerMiB);

            log.info(
                    "Creating cache {} with max extries: {}, concurrency level {}, expiration time: {} minutes",
                    name,
                    maxEntries,
                    concurrencyLevel,
                    expirationMinutes);

            return CacheBuilder.newBuilder()
                    .concurrencyLevel(concurrencyLevel)
                    .softValues()
                    .expireAfterAccess(expirationMinutes, TimeUnit.MINUTES)
                    .maximumSize(maxEntries)
                    .build();
        }
    }

    // <bean id="JDBCConfigXStreamPersisterInitializer"
    // class="org.geoserver.jdbcconfig.internal.JDBCConfigXStreamPersisterInitializer"/>
    @DependsOn("jdbcConfigDataSourceStartupValidator")
    @Bean("JDBCConfigXStreamPersisterInitializer")
    JDBCConfigXStreamPersisterInitializer jdbcConfigXStreamPersisterInitializer() {
        return new JDBCConfigXStreamPersisterInitializer();
    }

    @ConditionalOnMissingBean(ResourceNotificationDispatcher.class)
    @Bean
    ResourceNotificationDispatcher resourceNotificationDispatcher() {
        return new SimpleResourceNotificationDispatcher();
    }

    /**
     * Checks for {@link #jdbcConfigDataSource()} connectivity, delaying startup until the database
     * is ready, as long as the beans that depend on {@literal jdbcConfigDataSource} are annotated
     * with {@literal @DependsOn("jdbcConfigDataSourceStartupValidator")}
     *
     * @see DatabaseStartupValidator
     */
    @Bean
    DatabaseStartupValidator jdbcConfigDataSourceStartupValidator() {
        DatabaseStartupValidator jdbcConfigDataSourceValidator = new DatabaseStartupValidator();
        jdbcConfigDataSourceValidator.setDataSource(jdbcConfigDataSource());
        return jdbcConfigDataSourceValidator;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    static class ExtendedDataSourceProperties extends DataSourceProperties {
        int minimumIdle = 2;
        int maximumPoolSize = 10;
        long connectionTimeout = 250; // ms
        long idleTimeout = 60_000; // ms
    }

    @Bean
    @ConfigurationProperties("geoserver.backend.jdbcconfig.datasource")
    ExtendedDataSourceProperties jdbcconfigDataSourceProperties() {
        return new ExtendedDataSourceProperties();
    }

    @Bean(name = {"jdbcConfigDataSource", "jdbcStoreDataSource"})
    @ConfigurationProperties(prefix = "geoserver.backend.jdbcconfig.datasource")
    DataSource jdbcConfigDataSource() {
        ExtendedDataSourceProperties props = jdbcconfigDataSourceProperties();
        HikariDataSource dataSource =
                props.initializeDataSourceBuilder() //
                        .type(HikariDataSource.class)
                        .build();

        dataSource.setMaximumPoolSize(props.getMaximumPoolSize());
        dataSource.setMinimumIdle(props.getMinimumIdle());
        dataSource.setConnectionTimeout(props.getConnectionTimeout());
        dataSource.setIdleTimeout(props.getIdleTimeout());

        log.info(
                "jdbcconfig datasource: url: {}, user: {}, max size: {}, min size: {}, connection timeout: {}, idle timeout: {}",
                props.getUrl(),
                props.getUsername(),
                props.getMaximumPoolSize(),
                props.getMinimumIdle(),
                props.getConnectionTimeout(),
                props.getIdleTimeout());
        return dataSource;
    }

    @Bean
    @DependsOn("jdbcConfigDataSourceStartupValidator")
    DataSourceTransactionManager jdbcConfigTransactionManager(
            @Qualifier("jdbcConfigDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
