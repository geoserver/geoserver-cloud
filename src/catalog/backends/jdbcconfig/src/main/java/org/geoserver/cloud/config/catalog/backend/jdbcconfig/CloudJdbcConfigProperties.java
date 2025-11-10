/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.config.catalog.backend.jdbcconfig;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.Serial;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import lombok.EqualsAndHashCode;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.jdbcconfig.internal.JDBCConfigProperties;
import org.geoserver.jdbcconfig.internal.JDBCConfigPropertiesFactoryBean;
import org.geoserver.jdbcloader.DataSourceFactoryBean;
import org.geoserver.platform.resource.Resource;

/** Extends {@link JDBCConfigProperties} to not need a {@link JDBCConfigPropertiesFactoryBean} */
@EqualsAndHashCode(callSuper = true)
public class CloudJdbcConfigProperties extends JDBCConfigProperties {
    @Serial
    private static final long serialVersionUID = 1L;

    private transient DataSource dataSource;

    public CloudJdbcConfigProperties(DataSource dataSource) {
        super((JDBCConfigPropertiesFactoryBean) null);
        this.dataSource = dataSource;
    }

    /** Override to not save at all */
    @Override
    public void save() throws IOException {
        // no-op
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
                if (dbSchemaExists(st)) {
                    initDb = false;
                }
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
        return initDb;
    }

    private boolean dbSchemaExists(Statement st) {
        try (ResultSet rs = st.executeQuery("select count(*) from object_property")) {
            return true;
        } catch (SQLException e) {
            // table not found, proceed with initialization
            return false;
        }
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

        return org.geoserver.platform.resource.URIs.asResource(initScript);
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
