package org.geoserver.cloud.autoconfigure.jdbcconfig;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.jdbcconfig.JDBCGeoServerLoader;
import org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade;
import org.geoserver.jdbcconfig.config.JDBCGeoServerFacade;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.jdbcconfig.internal.JDBCCacheProvider;
import org.geoserver.jdbcconfig.internal.JDBCConfigProperties;
import org.geoserver.jdbcconfig.internal.JDBCConfigPropertiesFactoryBean;
import org.geoserver.jdbcconfig.internal.JDBCConfigXStreamPersisterInitializer;
import org.geoserver.jdbcconfig.internal.XStreamInfoSerialBinding;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.DataDirectoryResourceStore;
import org.geoserver.platform.resource.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnClass(JDBCConfigProperties.class)
@ConditionalOnProperty(prefix = "geoserver.jdbcconfig", name = "enabled", matchIfMissing = true)
@Import(JDBCConfigWebAutoConfiguration.class)
@EnableTransactionManagement
public class JDBCConfigAutoConfiguration {

    /**
     * Extends {@link JDBCConfigProperties} to not need a {@link JDBCConfigPropertiesFactoryBean}
     */
    public static class CloudJdbcConfigProperties extends JDBCConfigProperties {
        private static final long serialVersionUID = 1L;
        private DataSource dataSource;

        public CloudJdbcConfigProperties(DataSource dataSource) {
            super((JDBCConfigPropertiesFactoryBean) null);
            this.dataSource = dataSource;
        }

        /** Override to not save at all */
        public @Override void save() throws IOException {
            // factory.saveConfig(this);
        }

        /** Override to return {@code true} only if the db schema is not already created */
        public @Override boolean isInitDb() {
            //	        return Boolean.parseBoolean(getProperty("initdb", "false"));
            boolean initDb = super.isInitDb();
            if (initDb) {
                try (Connection c = dataSource.getConnection();
                        Statement st = c.createStatement()) {
                    try {
                        st.executeQuery("select * from object_property");
                        initDb = false;
                    } catch (SQLException e) {
                        // table not found, proceed with initialization
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            return initDb;
        }

        /**
         * Override to get the init script directly from the ones in the classpath (inside
         * gs-jdbcconfig.jar)
         */
        public @Override Resource getInitScript() {
            final String driverClassName = getProperty("datasource.driverClassname");
            String scriptName;
            switch (driverClassName) {
                case "org.h2.Driver":
                    scriptName = "initdb.h2.sql";
                    break;
                case "org.postgresql.Driver":
                    scriptName = "initdb.postgres.sql";
                    break;
                default:
                    scriptName = null;
            }
            if (scriptName == null) {
                return null;
            }
            URL initScript = ConfigDatabase.class.getResource(scriptName);
            Preconditions.checkState(
                    initScript != null,
                    "Init script does not exist: %s/%s",
                    ConfigDatabase.class.getPackage().getName(),
                    scriptName);

            Resource resource = org.geoserver.platform.resource.URIs.asResource(initScript);
            return resource;
        }

        public @Override Optional<String> getJdbcUrl() {
            String url = getProperty("datasource.url");
            if (null == url) {
                url = getProperty("datasource.jdbcUrl");
            }
            return Optional.fromNullable(url);
        }
    }

    //	  <!-- main configuration, loaded via factory bean -->
    //	  <bean id="jdbcConfigProperties"
    //	    class="org.geoserver.jdbcconfig.internal.JDBCConfigPropertiesFactoryBean">
    //	      <constructor-arg ref="resourceStore"/>
    //	  </bean>
    @ConfigurationProperties(prefix = "geoserver.jdbcconfig")
    public @Bean("JDBCConfigProperties") JDBCConfigProperties jdbcConfigProperties(
            @Qualifier("jdbcConfigDataSource") DataSource dataSource) {
        JDBCConfigProperties props = new CloudJdbcConfigProperties(dataSource);
        return props;
    }

    //	  <!-- data source, also loaded and configured via factory bean -->
    //	  <bean id="jdbcConfigDataSource" class="org.geoserver.jdbcloader.DataSourceFactoryBean">
    //	    <constructor-arg ref="jdbcConfigProperties" />
    //	  </bean>
    @Bean
    @ConfigurationProperties(prefix = "geoserver.jdbcconfig.datasource")
    public DataSource jdbcConfigDataSource() {
        return DataSourceBuilder.create().build();
    }

    //	  <!-- transaction manager -->
    //	  <bean id="jdbcConfigTransactionManager"
    //	  	class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
    //	    <property name="dataSource" ref="jdbcConfigDataSource" />
    //	  </bean>
    //	  <tx:annotation-driven /> -> @EnableTransactionManagement above
    @Bean
    public DataSourceTransactionManager jdbcConfigTransactionManager() {
        return new DataSourceTransactionManager(jdbcConfigDataSource());
    }

    //	  <bean id="jdbcPersistenceBinding"
    // class="org.geoserver.jdbcconfig.internal.XStreamInfoSerialBinding">
    //	    <constructor-arg ref="xstreamPersisterFactory" />
    //	  </bean>
    public @Bean XStreamInfoSerialBinding jdbcPersistenceBinding(
            @Qualifier("xstreamPersisterFactory") XStreamPersisterFactory xstreamPersisterFactory) {
        return new XStreamInfoSerialBinding(xstreamPersisterFactory);
    }

    //	  <bean id="JDBCConfigDB" class="org.geoserver.jdbcconfig.internal.ConfigDatabase">
    //	    <constructor-arg ref="jdbcConfigDataSource" />
    //	    <constructor-arg ref="jdbcPersistenceBinding" />
    //	  </bean>
    public @Bean(name = "JDBCConfigDB") ConfigDatabase jdbcConfigDB(
            @Qualifier("jdbcConfigDataSource") DataSource dataSource,
            XStreamInfoSerialBinding binding) {
        return new ConfigDatabase(dataSource, binding);
    }

    //	  <bean id="JDBCCatalogFacade" class="org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade">
    //	    <constructor-arg ref="JDBCConfigDB" />
    //	  </bean>
    public @Bean(name = "JDBCCatalogFacade") JDBCCatalogFacade jdbcCatalogFacade(
            ConfigDatabase configDb) {
        return new JDBCCatalogFacade(configDb);
    }

    //	  <bean id="JDBCGeoServerFacade" class="org.geoserver.jdbcconfig.config.JDBCGeoServerFacade">
    //	    <constructor-arg ref="JDBCConfigDB" />
    //	    <property name="resourceLoader" ref="resourceLoader" />
    //	    <property name="ddResourceStore" ref="dataDirectoryResourceStore" />
    //	  </bean>
    public @Bean("JDBCGeoServerFacade") JDBCGeoServerFacade jdbcGeoServerFacade(
            ConfigDatabase configDb,
            GeoServerResourceLoader resourceLoader,
            DataDirectoryResourceStore ddResourceStore) {

        JDBCGeoServerFacade facade = new JDBCGeoServerFacade(configDb);
        facade.setResourceLoader(resourceLoader);
        facade.setDdResourceStore(ddResourceStore);
        return facade;
    }

    //	  <bean id="JDBCGeoServerLoader" class="org.geoserver.jdbcconfig.JDBCGeoServerLoader">
    //	    <description>
    //	      Replaces the default GeoServerLoader to establish the JDBCCatalogFacade and
    // JDBCGeoServerFacade
    //	    </description>
    //	    <constructor-arg ref="resourceLoader" />
    //	    <constructor-arg ref="jdbcConfigProperties" />
    //	    <property name="catalogFacade" ref="JDBCCatalogFacade" />
    //	    <property name="geoServerFacade" ref="JDBCGeoServerFacade" />
    //	  </bean>
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

    //	  <bean id="JDBCCacheProvider" class="org.geoserver.jdbcconfig.internal.JDBCCacheProvider"/>
    public @Bean("JDBCCacheProvider") JDBCCacheProvider jdbcCacheProvider() {
        return new JDBCCacheProvider();
    }

    //	    <bean id="JDBCConfigXStreamPersisterInitializer"
    // class="org.geoserver.jdbcconfig.internal.JDBCConfigXStreamPersisterInitializer"/>
    public @Bean("JDBCConfigXStreamPersisterInitializer") JDBCConfigXStreamPersisterInitializer
            jdbcConfigXStreamPersisterInitializer() {
        return new JDBCConfigXStreamPersisterInitializer();
    }
}
