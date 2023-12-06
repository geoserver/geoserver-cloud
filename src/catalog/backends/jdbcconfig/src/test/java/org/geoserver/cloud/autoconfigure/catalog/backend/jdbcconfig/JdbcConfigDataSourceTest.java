/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.jdbcconfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

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
class JdbcConfigDataSourceTest extends JDBCConfigTest {

    @Test
    void testDataSource() throws SQLException {
        DataSource ds = context.getBean("jdbcConfigDataSource", DataSource.class);
        assertSame(ds, context.getBean("jdbcStoreDataSource", DataSource.class));
        assertThat(ds, instanceOf(HikariDataSource.class));
        @SuppressWarnings("resource")
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
