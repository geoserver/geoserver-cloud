/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.jdbcconfig;

import java.sql.Driver;
import javax.sql.DataSource;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.jdbcconfig.JDBCGeoServerLoader;
import org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade;
import org.geoserver.jdbcconfig.config.JDBCGeoServerFacade;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.jdbcconfig.internal.JDBCCacheProvider;
import org.geoserver.jdbcconfig.internal.JDBCConfigProperties;
import org.geoserver.jdbcconfig.internal.JDBCConfigXStreamPersisterInitializer;
import org.geoserver.jdbcconfig.internal.XStreamInfoSerialBinding;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.DataDirectoryResourceStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.bus.ConditionalOnBusEnabled;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Configure JDBC Config catalog
 *
 * <p>Configuration properties:
 *
 * <ul>
 *   <li>{@code geoserver.jdbcconfig.enabled}: whether to enable jdbc-config, provided it's in the
 *       classpath. Defaults to {@code true}
 *   <li>{@code geoserver.jdbcconfig.web.enabled}: whether to enable jdbc-config wicket components,
 *       provided {@code jdbcconfig} is enabled and the geoserver web-ui is in the classpath.
 *       Defaults to {@code true}.
 *   <li>{@code geoserver.jdbcconfig.datasource.jdbcUrl}: connection URL (e.g. {@code
 *       jdbc:postgresql://localhost:5432/gsconfigdb})
 *   <li>{@code geoserver.jdbcconfig.datasource.username}:
 *   <li>{@code geoserver.jdbcconfig.datasource.password}:
 *   <li>{@code geoserver.jdbcconfig.datasource.driverClassname}: JDBC {@link Driver} class name
 *       (e.g. {@code org.postgresql.Driver})
 * </ul>
 *
 * The {@link DataSource} is provided using regular spring-boot data source configuration properties
 * using the {@code geoserver.jbcconfig.datasource} property name prefix (see {@link
 * JDBCDataSourceConfiguration}).
 */
@ConditionalOnClass(JDBCCatalogFacade.class)
@ConditionalOnProperty(prefix = "geoserver.jdbcconfig", name = "enabled", matchIfMissing = true)
@Import({JDBCDataSourceConfiguration.class, JDBCConfigWebConfiguration.class})
public class JDBCConfigConfiguration {

    private @Value("${geoserver.jdbcconfig.datasource.jdbcUrl}") String jdbcUrl;

    @Bean
    @ConditionalOnBusEnabled
    public JdbcConfigRemoteEventProcessor jdbcConfigRemoteEventProcessor() {
        return new JdbcConfigRemoteEventProcessor();
    }

    // <!-- main configuration, loaded via factory bean -->
    // <bean id="jdbcConfigProperties"
    // class="org.geoserver.jdbcconfig.internal.JDBCConfigPropertiesFactoryBean">
    // <constructor-arg ref="resourceStore"/>
    // </bean>
    @ConfigurationProperties(prefix = "geoserver.jdbcconfig")
    public @Bean("JDBCConfigProperties") JDBCConfigProperties jdbcConfigProperties(
            @Qualifier("jdbcConfigDataSource") DataSource dataSource) {
        CloudJdbcConfigProperties configProperties = new CloudJdbcConfigProperties(dataSource);
        // dataSourceId shows up in geoserver's home page so set it here
        configProperties.setDatasourceId(jdbcUrl);
        return configProperties;
    }

    // <bean id="jdbcPersistenceBinding"
    // class="org.geoserver.jdbcconfig.internal.XStreamInfoSerialBinding">
    // <constructor-arg ref="xstreamPersisterFactory" />
    // </bean>
    public @Bean XStreamInfoSerialBinding jdbcPersistenceBinding(
            @Qualifier("xstreamPersisterFactory") XStreamPersisterFactory xstreamPersisterFactory) {
        return new XStreamInfoSerialBinding(xstreamPersisterFactory);
    }

    // <bean id="JDBCConfigDB"
    // class="org.geoserver.jdbcconfig.internal.ConfigDatabase">
    // <constructor-arg ref="jdbcConfigDataSource" />
    // <constructor-arg ref="jdbcPersistenceBinding" />
    // </bean>
    public @Bean(name = "JDBCConfigDB") ConfigDatabase jdbcConfigDB(
            @Qualifier("jdbcConfigDataSource") DataSource dataSource,
            XStreamInfoSerialBinding binding) {
        return new ConfigDatabase(dataSource, binding);
    }

    // <bean id="JDBCCatalogFacade"
    // class="org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade">
    // <constructor-arg ref="JDBCConfigDB" />
    // </bean>
    public @Bean(name = "JDBCCatalogFacade") JDBCCatalogFacade jdbcCatalogFacade(
            ConfigDatabase configDb) {
        return new JDBCCatalogFacade(configDb);
    }

    // <bean id="JDBCGeoServerFacade"
    // class="org.geoserver.jdbcconfig.config.JDBCGeoServerFacade">
    // <constructor-arg ref="JDBCConfigDB" />
    // <property name="resourceLoader" ref="resourceLoader" />
    // <property name="ddResourceStore" ref="dataDirectoryResourceStore" />
    // </bean>
    public @Bean("JDBCGeoServerFacade") JDBCGeoServerFacade jdbcGeoServerFacade(
            ConfigDatabase configDb,
            GeoServerResourceLoader resourceLoader,
            DataDirectoryResourceStore ddResourceStore) {

        JDBCGeoServerFacade facade = new JDBCGeoServerFacade(configDb);
        facade.setResourceLoader(resourceLoader);
        facade.setDdResourceStore(ddResourceStore);
        return facade;
    }

    // <bean id="JDBCGeoServerLoader"
    // class="org.geoserver.jdbcconfig.JDBCGeoServerLoader">
    // <description>
    // Replaces the default GeoServerLoader to establish the JDBCCatalogFacade and
    // JDBCGeoServerFacade
    // </description>
    // <constructor-arg ref="resourceLoader" />
    // <constructor-arg ref="jdbcConfigProperties" />
    // <property name="catalogFacade" ref="JDBCCatalogFacade" />
    // <property name="geoServerFacade" ref="JDBCGeoServerFacade" />
    // </bean>
    public @Bean("JDBCGeoServerLoader") JDBCGeoServerLoader jdbcGeoServerLoader( //
            GeoServerResourceLoader resourceLoader, //
            JDBCConfigProperties config, //
            JDBCCatalogFacade catalogFacade, //
            JDBCGeoServerFacade geoServerFacade)
            throws Exception {

        JDBCGeoServerLoader loader = new JDBCGeoServerLoader(resourceLoader, config);
        loader.setCatalogFacade(catalogFacade);
        loader.setGeoServerFacade(geoServerFacade);
        return loader;
    }

    // <bean id="JDBCCacheProvider"
    // class="org.geoserver.jdbcconfig.internal.JDBCCacheProvider"/>
    public @Bean("JDBCCacheProvider") JDBCCacheProvider jdbcCacheProvider() {
        return new JDBCCacheProvider();
    }

    // <bean id="JDBCConfigXStreamPersisterInitializer"
    // class="org.geoserver.jdbcconfig.internal.JDBCConfigXStreamPersisterInitializer"/>
    public @Bean("JDBCConfigXStreamPersisterInitializer") JDBCConfigXStreamPersisterInitializer
            jdbcConfigXStreamPersisterInitializer() {
        return new JDBCConfigXStreamPersisterInitializer();
    }
}
