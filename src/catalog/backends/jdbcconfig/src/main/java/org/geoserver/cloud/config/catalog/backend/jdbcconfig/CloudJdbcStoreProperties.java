/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.jdbcconfig;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.geoserver.jdbcloader.DataSourceFactoryBean;
import org.geoserver.jdbcstore.internal.JDBCResourceStoreProperties;
import org.geoserver.jdbcstore.internal.JDBCResourceStorePropertiesFactoryBean;
import org.geoserver.platform.resource.Resource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

/**
 * Extends {@link JDBCResourceStoreProperties} to not need a {@link
 * JDBCResourceStorePropertiesFactoryBean}
 */
public class CloudJdbcStoreProperties extends JDBCResourceStoreProperties {
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_CACHE_DIRECTORY =
            System.getProperty("java.io.tmpdir") + File.separator + "geoserver-jdbcconfig-cache";

    private DataSource dataSource;

    public CloudJdbcStoreProperties(DataSource dataSource) {
        super((JDBCResourceStorePropertiesFactoryBean) null);
        this.dataSource = dataSource;
    }

    public File getCacheDirectory() {
        String location = super.getProperty("cache-directory", DEFAULT_CACHE_DIRECTORY);
        File cacheDirectory = new File(location);
        cacheDirectory.mkdirs();
        return cacheDirectory;
    }

    /**
     * Override to not save at all, the canonical source of config settings is the spring boot
     * configuration properties
     */
    @Override
    public void save() throws IOException {
        // factory.saveConfig(this);
    }

    /** Override to return {@code true} only if the db schema is not already created */
    @Override
    public boolean isInitDb() {
        boolean initDb = Boolean.parseBoolean(getProperty("initdb", "false"));
        if (initDb) {
            try (Connection c = dataSource.getConnection();
                    Statement st = c.createStatement()) {
                try {
                    st.executeQuery("select count(*) from resources");
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
    @Override
    public Resource getInitScript() {
        final String driverClassName = getProperty("datasource.driverClassname");
        String scriptName;
        switch (driverClassName) {
            case "org.h2.Driver":
                scriptName = "init.h2.sql";
                break;
            case "org.postgresql.Driver":
                scriptName = "init.postgres.sql";
                break;
            default:
                scriptName = null;
        }
        if (scriptName == null) {
            return null;
        }
        URL initScript = JDBCResourceStoreProperties.class.getResource(scriptName);
        Preconditions.checkState(
                initScript != null,
                "Init script does not exist: %s/%s",
                JDBCResourceStoreProperties.class.getPackage().getName(),
                scriptName);

        Resource resource = org.geoserver.platform.resource.URIs.asResource(initScript);
        return resource;
    }

    /**
     * Override to throw an {@link UnsupportedOperationException}, we're not using {@link
     * DataSourceFactoryBean}, the datasource is provided by spring instead
     */
    @Override
    public Optional<String> getJdbcUrl() {
        throw new UnsupportedOperationException(
                "shouldn't be called, this module doesn't use org.geoserver.jdbcloader.DataSourceFactoryBean");
    }
}
