/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.test.backend;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.junit.After;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Closes the {@link DataSource} after each test run, otherwise, being an in-memory H2 db (as
 * configured in {@code bootstrap-test.yml}) the first test class will succeed and the following
 * ones will fail, looks like the db got empty but wasn't closed.
 */
public abstract class JDBCConfigTest extends GeoServerBackendConfigurerTest {

    protected @Autowired @Qualifier("jdbcConfigDataSource") DataSource dataSource;

    public @After void close() {
        ((HikariDataSource) dataSource).close();
    }
}
