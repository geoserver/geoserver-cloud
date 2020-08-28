/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.jdbcconfig;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.geoserver.jdbcloader.DataSourceFactoryBean;
import org.geoserver.jdbcstore.internal.JDBCResourceStoreProperties;
import org.geoserver.jdbcstore.internal.JDBCResourceStorePropertiesFactoryBean;
import org.geoserver.platform.resource.Resource;

/**
 * Extends {@link JDBCResourceStoreProperties} to not need a {@link
 * JDBCResourceStorePropertiesFactoryBean}
 */
public class CloudJdbcStoreProperties extends JDBCResourceStoreProperties {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    public CloudJdbcStoreProperties(DataSource dataSource) {
        super((JDBCResourceStorePropertiesFactoryBean) null);
        this.dataSource = dataSource;
    }

    /** Override to not save at all */
    public @Override void save() throws IOException {
        // factory.saveConfig(this);
    }

    /** Override to return {@code true} only if the db schema is not already created */
    public @Override boolean isInitDb() {
        // return Boolean.parseBoolean(getProperty("initdb", "false"));
        boolean initDb = super.isInitDb();
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
    public @Override Resource getInitScript() {
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
    public @Override Optional<String> getJdbcUrl() {
        throw new UnsupportedOperationException(
                "shouldn't be called, this module doesn't use org.geoserver.jdbcloader.DataSourceFactoryBean");
    }
}