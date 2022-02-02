/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.jdbcconfig;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.sql.Driver;
import javax.sql.DataSource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.plugin.CatalogFacadeExtensionAdapter;
import org.geoserver.catalog.plugin.ExtendedCatalogFacade;
import org.geoserver.cloud.autoconfigure.bus.ConditionalOnGeoServerRemoteEventsEnabled;
import org.geoserver.cloud.config.catalog.GeoServerBackendConfigurer;
import org.geoserver.cloud.config.catalog.GeoServerBackendProperties;
import org.geoserver.cloud.config.jdbcconfig.bus.JdbcConfigRemoteEventProcessor;
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
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.SimpleResourceNotificationDispatcher;
import org.geoserver.util.CacheProvider;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
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
 * geoserver.backend.jdbcconfig.datasource.configuration
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
 * }</pre>
 */
@Configuration(proxyBeanMethods = true)
@EnableTransactionManagement
@EnableConfigurationProperties
@Slf4j(topic = "org.geoserver.cloud.config.jdbcconfig")
public class JDBCConfigBackendConfigurer implements GeoServerBackendConfigurer {

    private String instanceId;
    private GeoServerBackendProperties backendConfig;

    @Autowired
    public JDBCConfigBackendConfigurer(
            GeoServerBackendProperties backendConfig,
            @Value("${info.instance-id:}") String instanceId) {
        this.backendConfig = backendConfig;
        this.instanceId = instanceId;
        log.info(
                "Loading geoserver config backend with {}",
                JDBCConfigBackendConfigurer.class.getSimpleName());
    }

    @DependsOn("jdbcConfigDataSourceStartupValidator")
    @ConfigurationProperties(prefix = "geoserver.backend.jdbcconfig")
    public @Bean("JDBCConfigProperties") JDBCConfigProperties jdbcConfigProperties() {
        DataSource dataSource = jdbcConfigDataSource();
        CloudJdbcConfigProperties props = new CloudJdbcConfigProperties(dataSource);

        // dataSourceId shows up in geoserver's home page so set it here
        GeoServerBackendProperties backendProperties = backendConfig;
        DataSourceProperties dataSourceProperties =
                backendProperties.getJdbcconfig().getDatasource();
        String jdbcUrl = dataSourceProperties.getUrl();
        props.setDatasourceId(jdbcUrl);
        return props;
    }

    @DependsOn("jdbcConfigDataSourceStartupValidator")
    @ConfigurationProperties(prefix = "geoserver.backend.jdbcconfig")
    public @Bean CloudJdbcStoreProperties jdbcStoreProperties() {
        return new CloudJdbcStoreProperties(jdbcConfigDataSource());
    }

    @DependsOn({"extensions", "jdbcConfigDataSourceStartupValidator"})
    public @Override @Bean GeoServerResourceLoader resourceLoader() {
        GeoServerBackendProperties configProperties = backendConfig;
        Path path = configProperties.getDataDirectory().getLocation();
        File dataDirectory = path == null ? null : path.toFile();
        GeoServerResourceLoader loader = new GeoServerResourceLoader(resourceStoreImpl());
        loader.setBaseDirectory(dataDirectory);
        return loader;
    }

    @DependsOn({"extensions", "JDBCConfigDB", "jdbcConfigDataSourceStartupValidator"})
    @Bean(name = {"resourceStoreImpl"})
    public @Override @NonNull ResourceStore resourceStoreImpl() {
        final JDBCConfigProperties jdbcConfigProperties = jdbcConfigProperties();
        final CloudJdbcStoreProperties jdbcStoreProperties = jdbcStoreProperties();
        final ConfigDatabase jdbcConfigDB = jdbcConfigDB();

        initDbSchema(jdbcConfigProperties, jdbcStoreProperties, jdbcConfigDB);
        try {
            // no fall back to data directory, jdbcconfig is either enabled and fully engaged, or
            // not at all
            JDBCResourceStore resourceStore;
            resourceStore = new JDBCResourceStore(jdbcConfigDataSource(), jdbcStoreProperties);
            resourceStore.setCache(jdbcResourceCache());
            resourceStore.setLockProvider(jdbcStoreLockProvider());
            resourceStore.setResourceNotificationDispatcher(resourceNotificationDispatcher());
            return resourceStore;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @DependsOn({"extensions", "jdbcConfigDataSourceStartupValidator"})
    public @Bean LockRegistryAdapter jdbcStoreLockProvider() {
        return new LockRegistryAdapter(jdbcLockRegistry());
    }

    @DependsOn("jdbcConfigDataSourceStartupValidator")
    public @Bean JdbcLockRegistry jdbcLockRegistry() {
        return new JdbcLockRegistry(jdbcLockRepository());
    }

    @DependsOn("jdbcConfigDataSourceStartupValidator")
    public @Bean DefaultLockRepository jdbcLockRepository() {
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
    public @Override ExtendedCatalogFacade catalogFacade() {
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
    public @Override GeoServerFacade geoserverFacade() {
        initDbSchema(jdbcConfigProperties(), jdbcStoreProperties(), jdbcConfigDB());
        ConfigDatabase configDB = jdbcConfigDB();
        CloudJdbcGeoserverFacade facade = new CloudJdbcGeoserverFacade(configDB);
        return facade;
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
    public @Override CloudJdbcGeoServerLoader geoServerLoaderImpl() {
        JDBCConfigProperties config = jdbcConfigProperties();
        ConfigDatabase configdb = jdbcConfigDB();
        try {
            return new CloudJdbcGeoServerLoader(resourceLoader(), config, configdb);
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
    @ConditionalOnGeoServerRemoteEventsEnabled
    public JdbcConfigRemoteEventProcessor jdbcConfigRemoteEventProcessor() {
        return new JdbcConfigRemoteEventProcessor();
    }

    public @Bean XStreamPersisterFactory xstreamPersisterFactory() {
        return new XStreamPersisterFactory();
    }

    @DependsOn({"jdbcConfigDataSourceStartupValidator", "xstreamPersisterFactory"})
    public @Bean XStreamInfoSerialBinding jdbcPersistenceBinding() {
        return new XStreamInfoSerialBinding(xstreamPersisterFactory());
    }

    @DependsOn({"jdbcConfigDataSourceStartupValidator", "jdbcConfigDataSource"})
    public @Bean(name = "JDBCConfigDB") ConfigDatabase jdbcConfigDB() {
        ConfigDatabase configDb =
                new CloudJdbcConfigDatabase(jdbcConfigDataSource(), jdbcPersistenceBinding());
        return configDb;
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

                    public @Override void runScript(Resource script) {
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
    public @Bean("JDBCCacheProvider") CacheProvider jdbcCacheProvider() {
        // return new JDBCCacheProvider();
        return new CacheProvider() {
            public @Override <K extends Serializable, V extends Serializable> Cache<K, V> getCache(
                    String cacheName) {
                return CacheBuilder.newBuilder().maximumSize(0).build();
            }
        };
    }

    // <bean id="JDBCConfigXStreamPersisterInitializer"
    // class="org.geoserver.jdbcconfig.internal.JDBCConfigXStreamPersisterInitializer"/>
    @DependsOn("jdbcConfigDataSourceStartupValidator")
    public @Bean("JDBCConfigXStreamPersisterInitializer") JDBCConfigXStreamPersisterInitializer
            jdbcConfigXStreamPersisterInitializer() {
        return new JDBCConfigXStreamPersisterInitializer();
    }

    @ConditionalOnMissingBean(ResourceNotificationDispatcher.class)
    public @Bean ResourceNotificationDispatcher resourceNotificationDispatcher() {
        return new SimpleResourceNotificationDispatcher();
    }

    /**
     * Checks for {@link #jdbcConfigDataSource()} connectivity, delaying startup until the database
     * is ready, as long as the beans that depend on {@literal jdbcConfigDataSource} are annotated
     * with {@literal @DependsOn("jdbcConfigDataSourceStartupValidator")}
     *
     * @see DatabaseStartupValidator
     */
    public @Bean DatabaseStartupValidator jdbcConfigDataSourceStartupValidator() {
        DatabaseStartupValidator jdbcConfigDataSourceValidator = new DatabaseStartupValidator();
        jdbcConfigDataSourceValidator.setDataSource(jdbcConfigDataSource());
        return jdbcConfigDataSourceValidator;
    }

    @Bean
    @ConfigurationProperties("geoserver.backend.jdbcconfig.datasource")
    public DataSourceProperties jdbcconfigDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = {"jdbcConfigDataSource", "jdbcStoreDataSource"})
    // @ConfigurationProperties(prefix = "geoserver.backend.jdbcconfig.datasource")
    public DataSource jdbcConfigDataSource() {
        return jdbcconfigDataSourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        // String url = env.getProperty("geoserver.backend.jdbcconfig.datasource.jdbc-url");
        // return DataSourceBuilder.create().build();
    }

    @Bean
    @DependsOn("jdbcConfigDataSourceStartupValidator")
    public DataSourceTransactionManager jdbcConfigTransactionManager(
            @Qualifier("jdbcConfigDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
