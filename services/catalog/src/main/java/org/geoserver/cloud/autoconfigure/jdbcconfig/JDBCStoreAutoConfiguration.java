/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.jdbcconfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.sql.DataSource;
import org.geoserver.jdbcstore.JDBCResourceStore;
import org.geoserver.jdbcstore.JDBCResourceStoreFactoryBean;
import org.geoserver.jdbcstore.cache.SimpleResourceCache;
import org.geoserver.jdbcstore.internal.JDBCResourceStoreProperties;
import org.geoserver.platform.resource.DataDirectoryResourceStore;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(JDBCResourceStore.class)
@ConditionalOnProperty(prefix = "geoserver.jdbcconfig", name = "enabled", matchIfMissing = true)
@Import(JDBCDataSourceConfiguration.class)
public class JDBCStoreAutoConfiguration {
    //	  <!-- main configuration, loaded via factory bean -->
    //	  <bean id="jdbcStoreProperties"
    //	    class="org.geoserver.jdbcstore.internal.JDBCResourceStorePropertiesFactoryBean">
    //	      <constructor-arg ref="dataDirectoryResourceStore"/>
    //	  </bean>
    @ConfigurationProperties(prefix = "geoserver.jdbcconfig")
    public @Bean JDBCResourceStoreProperties jdbcStoreProperties(
            @Qualifier("jdbcConfigDataSource") DataSource dataSource) {
        return new CloudJdbcStoreProperties(dataSource);
    }

    //	  <!-- cache -->
    //	  <bean id="resourceCache" class="org.geoserver.jdbcstore.cache.DataDirectoryResourceCache">
    //	  </bean>
    //	public @Bean DataDirectoryResourceCache resourceCache() {
    //		return new DataDirectoryResourceCache();
    //	}

    //	  <!-- resource store -->
    //	  <bean id="resourceStoreImpl" class="org.geoserver.jdbcstore.JDBCResourceStoreFactoryBean">
    //	    <constructor-arg ref="dataDirectoryResourceStore" />
    //	    <constructor-arg ref="jdbcStoreDataSource" />
    //	    <constructor-arg ref="jdbcStoreProperties" />
    //	    <property name="cache" ref="resourceCache" />
    //	    <property name="lockProvider" ref="lockProvider"/>
    //	    <property name="resourceNotificationDispatcher" ref="resourceNotificationDispatcher"/>
    //	  </bean>
    public @Bean JDBCResourceStoreFactoryBean resourceStoreImpl( //
            DataDirectoryResourceStore fallbackStore, //
            @Qualifier("jdbcStoreDataSource") DataSource jdbcStoreDataSource, //
            JDBCResourceStoreProperties config, //
            LockProvider lockProvider, //
            @Qualifier("resourceNotificationDispatcher")
                    ResourceNotificationDispatcher resourceWatcher)
            throws IOException {

        JDBCResourceStoreFactoryBean fac =
                new JDBCResourceStoreFactoryBean(fallbackStore, jdbcStoreDataSource, config);
        File base = Files.createTempDirectory("geoserver.jdbcresource.cache").toFile();
        fac.setCache(new SimpleResourceCache(base));
        fac.setLockProvider(lockProvider);
        fac.setResourceNotificationDispatcher(resourceWatcher);
        return fac;
    }
}
