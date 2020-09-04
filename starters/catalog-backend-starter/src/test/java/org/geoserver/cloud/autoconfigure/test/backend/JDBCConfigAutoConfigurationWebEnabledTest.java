/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.test.backend;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import org.geoserver.cloud.autoconfigure.catalog.JDBCConfigAutoConfiguration;
import org.geoserver.cloud.autoconfigure.testconfiguration.AutoConfigurationTestConfiguration;
import org.geoserver.cloud.config.jdbcconfig.JDBCConfigWebConfiguration;
import org.geoserver.jdbcconfig.web.JDBCConfigStatusProvider;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test {@link JDBCConfigWebConfiguration} through {@link JDBCConfigAutoConfiguration} when {@code
 * geoserver.backend.jdbcconfig.enabled=true} and {@code geoserver.backend.jdbcconfig.web=true}
 */
@SpringBootTest(
    classes = AutoConfigurationTestConfiguration.class,
    properties = {
        "geoserver.backend.jdbcconfig.enabled=true",
        "geoserver.backend.jdbcconfig.web.enabled=true"
    }
)
public class JDBCConfigAutoConfigurationWebEnabledTest extends JDBCConfigTest {

    @Test
    public void testJDBCConfigStatusProvider() {
        assertThat(
                context.getBean("JDBCConfigStatusProvider"),
                instanceOf(JDBCConfigStatusProvider.class));
    }
}
