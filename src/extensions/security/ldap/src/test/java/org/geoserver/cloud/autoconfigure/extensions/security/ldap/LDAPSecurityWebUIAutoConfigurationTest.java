/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.security.ldap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;
import org.geoserver.web.security.ldap.LDAPAuthProviderPanelInfo;
import org.geoserver.web.security.ldap.LDAPRoleServicePanelInfo;
import org.geoserver.web.security.ldap.LDAPUserGroupServicePanelInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * Test suite for {@link LDAPSecurityWebUIAutoConfiguration}
 *
 * @since 2.27.0.0
 */
class LDAPSecurityWebUIAutoConfigurationTest {

    private WebApplicationContextRunner runner;

    @BeforeEach
    void setup() {
        // Create a mock GeoServer instance to satisfy @ConditionalOnGeoServer
        var mockGeoServer = mock(GeoServer.class);

        runner = new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        LDAPSecurityAutoConfiguration.class, LDAPSecurityWebUIAutoConfiguration.class))
                .withBean("geoServer", GeoServer.class, () -> mockGeoServer)
                .withBean(GeoServerSecurityManager.class, () -> mock(GeoServerSecurityManager.class));
    }

    @Test
    void testConditionalOnClassNoMatch() {
        runner.withClassLoader(new FilteredClassLoader(AuthenticationFilterPanelInfo.class))
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(LDAPUserGroupServicePanelInfo.class)
                        .doesNotHaveBean(LDAPRoleServicePanelInfo.class)
                        .doesNotHaveBean(LDAPAuthProviderPanelInfo.class)
                        .doesNotHaveBean("ldapSecurityWebExtension"));
    }

    @Test
    void testConditionalOnClassMatch() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(LDAPUserGroupServicePanelInfo.class)
                .hasSingleBean(LDAPRoleServicePanelInfo.class)
                .hasSingleBean(LDAPAuthProviderPanelInfo.class)
                .hasBean("ldapSecurityWebExtension")
                .getBean("ldapSecurityWebExtension")
                .isInstanceOf(ModuleStatusImpl.class));
    }

    @Test
    void testDisabled() {
        runner.withPropertyValues("geoserver.extension.security.ldap.enabled=false")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(LDAPUserGroupServicePanelInfo.class)
                        .doesNotHaveBean(LDAPRoleServicePanelInfo.class)
                        .doesNotHaveBean(LDAPAuthProviderPanelInfo.class)
                        .doesNotHaveBean("ldapSecurityWebExtension"));
    }
}
