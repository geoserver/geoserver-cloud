/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.catalog.backend.jdbcconfig;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.jdbcconfig.internal.JDBCConfigProperties;
import org.geoserver.jdbcconfig.internal.JDBCConfigPropertiesFactoryBean;
import org.geoserver.jdbcloader.DataSourceFactoryBean;
import org.geoserver.platform.resource.Resource;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

/** Extends {@link JDBCConfigProperties} to not need a {@link JDBCConfigPropertiesFactoryBean} */
public class CloudJdbcConfigProperties extends JDBCConfigProperties {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    public CloudJdbcConfigProperties(DataSource dataSource) {
        super((JDBCConfigPropertiesFactoryBean) null);
        this.dataSource = dataSource;
    }

    /** Override to not save at all */
    @Override
    public void save() throws IOException {
        // factory.saveConfig(this);
    }

    public boolean isH2() {
        final String driverClassName = getProperty("datasource.driverClassname");
        return "org.h2.Driver".equals(driverClassName);
    }

    public boolean isPostgreSQL() {
        final String driverClassName = getProperty("datasource.driverClassname");
        return "org.postgresql.Driver".equals(driverClassName);
    }

    /** Override to return {@code true} only if the db schema is not already created */
    @Override
    public boolean isInitDb() {
        boolean initDb = Boolean.parseBoolean(getProperty("initdb", "false"));
        if (initDb) {
            try (Connection c = dataSource.getConnection();
                    Statement st = c.createStatement()) {
                try {
                    st.executeQuery("select count(*) from object_property");
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
        String scriptName;
        if (isH2()) {
            scriptName = "initdb.h2.sql";
        } else if (isPostgreSQL()) {
            scriptName = "initdb.postgres.sql";
        } else {
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
