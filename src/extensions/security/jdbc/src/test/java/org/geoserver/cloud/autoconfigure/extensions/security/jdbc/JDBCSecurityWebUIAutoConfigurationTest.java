/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;
import org.geoserver.security.web.jdbc.JDBCAuthProviderPanelInfo;
import org.geoserver.security.web.jdbc.JDBCRoleServicePanelInfo;
import org.geoserver.security.web.jdbc.JDBCUserGroupServicePanelInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * Test suite for {@link JDBCSecurityWebUIAutoConfiguration}
 *
 * @since 2.27.0.0
 */
class JDBCSecurityWebUIAutoConfigurationTest {

    private WebApplicationContextRunner runner;

    @BeforeEach
    void setup() {
        // Create a mock GeoServer instance to satisfy @ConditionalOnGeoServer
        var mockGeoServer = mock(GeoServer.class);

        runner = new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        JDBCSecurityAutoConfiguration.class, JDBCSecurityWebUIAutoConfiguration.class))
                .withBean("geoServer", GeoServer.class, () -> mockGeoServer)
                .withBean(GeoServerSecurityManager.class, () -> mock(GeoServerSecurityManager.class));
    }

    @Test
    void testConditionalOnClassNoMatch() {
        runner.withClassLoader(new FilteredClassLoader(AuthenticationFilterPanelInfo.class))
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(JDBCUserGroupServicePanelInfo.class)
                        .doesNotHaveBean(JDBCRoleServicePanelInfo.class)
                        .doesNotHaveBean(JDBCAuthProviderPanelInfo.class)
                        .doesNotHaveBean("jdbcSecurityWebExtension"));
    }

    @Test
    void testConditionalOnClassMatch() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(JDBCUserGroupServicePanelInfo.class)
                .hasSingleBean(JDBCRoleServicePanelInfo.class)
                .hasSingleBean(JDBCAuthProviderPanelInfo.class)
                .hasBean("jdbcSecurityWebExtension")
                .getBean("jdbcSecurityWebExtension")
                .isInstanceOf(ModuleStatusImpl.class));
    }

    @Test
    void testDisabled() {
        runner.withPropertyValues("geoserver.extension.security.jdbc.enabled=false")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(JDBCUserGroupServicePanelInfo.class)
                        .doesNotHaveBean(JDBCRoleServicePanelInfo.class)
                        .doesNotHaveBean(JDBCAuthProviderPanelInfo.class)
                        .doesNotHaveBean("jdbcSecurityWebExtension"));
    }
}
