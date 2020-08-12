package org.geoserver.cloud.autoconfigure.jdbcconfig;

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(JDBCCatalogFacade.class)
@ConditionalOnProperty(prefix = "geoserver.jdbcconfig", name = "enabled", matchIfMissing = true)
@Import({JDBCDataSourceConfiguration.class, JDBCConfigWebAutoConfiguration.class})
public class JDBCConfigAutoConfiguration {

    // <!-- main configuration, loaded via factory bean -->
    // <bean id="jdbcConfigProperties"
    // class="org.geoserver.jdbcconfig.internal.JDBCConfigPropertiesFactoryBean">
    // <constructor-arg ref="resourceStore"/>
    // </bean>
    @ConfigurationProperties(prefix = "geoserver.jdbcconfig")
    public @Bean("JDBCConfigProperties") JDBCConfigProperties jdbcConfigProperties(
            @Qualifier("jdbcConfigDataSource") DataSource dataSource) {
        return new CloudJdbcConfigProperties(dataSource);
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
