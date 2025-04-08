/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.security.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.config.GeoServer;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.jdbc.JDBCSecurityProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * Test suite for {@link JDBCSecurityAutoConfiguration}
 *
 * @since 2.27.0.0
 */
class JDBCSecurityAutoConfigurationTest {

    private WebApplicationContextRunner runner;

    @BeforeEach
    void setup() {
        // Create a mock GeoServer instance to satisfy @ConditionalOnGeoServer
        var mockGeoServer = mock(GeoServer.class);

        runner = new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JDBCSecurityAutoConfiguration.class))
                .withBean("geoServer", GeoServer.class, () -> mockGeoServer)
                .withBean(GeoServerSecurityManager.class, () -> mock(GeoServerSecurityManager.class));
    }

    @Test
    void testExpectedBeans() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(JDBCSecurityProvider.class)
                .getBean(JDBCConfigProperties.class)
                .hasFieldOrPropertyWithValue("enabled", true));
    }

    @Test
    void testDisabled() {
        runner.withPropertyValues("geoserver.extension.security.jdbc.enabled=false")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(JDBCSecurityProvider.class));
    }
}
