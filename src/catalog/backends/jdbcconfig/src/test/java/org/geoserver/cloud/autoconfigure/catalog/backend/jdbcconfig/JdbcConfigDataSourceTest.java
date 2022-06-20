/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.jdbcconfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import com.zaxxer.hikari.HikariDataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;

import javax.sql.DataSource;

@SpringBootTest(
        classes = AutoConfigurationTestConfiguration.class,
        properties = {
            "geoserver.backend.jdbcconfig.enabled=true",
            "geoserver.backend.jdbcconfig.datasource.maximumPoolSize=2",
            "geoserver.backend.jdbcconfig.datasource.minimumIdle=1",
            "geoserver.backend.jdbcconfig.datasource.connectionTimeout=250", // 250ms
            "geoserver.backend.jdbcconfig.datasource.idleTimeout=10000", // 10 secs
        })
@RunWith(SpringRunner.class)
public class JdbcConfigDataSourceTest extends JDBCConfigTest {

    public @Test void testDataSource() throws SQLException {
        DataSource ds = context.getBean("jdbcConfigDataSource", DataSource.class);
        assertSame(ds, context.getBean("jdbcStoreDataSource", DataSource.class));
        assertThat(ds, instanceOf(HikariDataSource.class));
        HikariDataSource hds = (HikariDataSource) ds;
        assertEquals(2, hds.getMaximumPoolSize());
        assertEquals(1, hds.getMinimumIdle());
        assertEquals(10_000, hds.getIdleTimeout());
        assertEquals(250, hds.getConnectionTimeout());
        try (Connection c1 = hds.getConnection();
                Connection c2 = hds.getConnection()) {
            assertThrows(SQLTransientConnectionException.class, () -> hds.getConnection());
        }
    }
}
