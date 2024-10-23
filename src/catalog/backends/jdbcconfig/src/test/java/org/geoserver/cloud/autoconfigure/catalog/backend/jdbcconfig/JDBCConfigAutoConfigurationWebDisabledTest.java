/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.catalog.backend.jdbcconfig;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.geoserver.cloud.config.catalog.backend.jdbcconfig.JDBCConfigWebConfiguration;
import org.geoserver.jdbcconfig.web.JDBCConfigStatusProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test {@link JDBCConfigWebConfiguration} through {@link JDBCConfigAutoConfiguration} when {@code
 * geoserver.backend.jdbcconfig.enabled=true} and {@code geoserver.backend.jdbcconfig.web=false}
 * (default behavior)
 */
@SpringBootTest(
        classes = AutoConfigurationTestConfiguration.class,
        properties = {"geoserver.backend.jdbcconfig.enabled=true", "geoserver.backend.jdbcconfig.web.enabled=false"})
class JDBCConfigAutoConfigurationWebDisabledTest extends JDBCConfigTest {

    @Test
    void testJDBCConfigStatusProvider() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(JDBCConfigStatusProvider.class));
    }
}
