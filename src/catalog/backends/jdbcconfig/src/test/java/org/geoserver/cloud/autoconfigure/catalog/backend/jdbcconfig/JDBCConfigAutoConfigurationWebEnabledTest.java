/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.jdbcconfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import org.geoserver.cloud.config.catalog.backend.jdbcconfig.JDBCConfigWebConfiguration;
import org.geoserver.jdbcconfig.web.JDBCConfigStatusProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test {@link JDBCConfigWebConfiguration} through {@link JDBCConfigAutoConfiguration} when {@code
 * geoserver.backend.jdbcconfig.enabled=true} and {@code geoserver.backend.jdbcconfig.web=true}
 */
@SpringBootTest(
        classes = AutoConfigurationTestConfiguration.class,
        properties = {
            "geoserver.backend.jdbcconfig.enabled=true",
            "geoserver.backend.jdbcconfig.web.enabled=true"
        })
@ActiveProfiles("test")
public class JDBCConfigAutoConfigurationWebEnabledTest extends JDBCConfigTest {

    @Test
    void testJDBCConfigStatusProvider() {
        assertThat(
                context.getBean("JDBCConfigStatusProvider"),
                instanceOf(JDBCConfigStatusProvider.class));
    }
}
