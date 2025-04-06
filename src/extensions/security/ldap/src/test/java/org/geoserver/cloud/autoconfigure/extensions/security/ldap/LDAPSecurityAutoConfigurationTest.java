/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.security.ldap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.config.GeoServer;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.ldap.LDAPSecurityProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * Test suite for {@link LDAPSecurityAutoConfiguration}
 *
 * @since 2.27.0.0
 */
class LDAPSecurityAutoConfigurationTest {

    private WebApplicationContextRunner runner;

    @BeforeEach
    void setup() {
        // Create a mock GeoServer instance to satisfy @ConditionalOnGeoServer
        var mockGeoServer = mock(GeoServer.class);

        runner = new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(LDAPSecurityAutoConfiguration.class))
                .withBean("geoServer", GeoServer.class, () -> mockGeoServer)
                .withBean(GeoServerSecurityManager.class, () -> mock(GeoServerSecurityManager.class));
    }

    @Test
    void testExpectedBeans() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(LDAPSecurityProvider.class)
                .getBean(LDAPConfigProperties.class)
                .hasFieldOrPropertyWithValue("enabled", true));
    }

    @Test
    void testDisabled() {
        runner.withPropertyValues("geoserver.extension.security.ldap.enabled=false")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(LDAPSecurityProvider.class));
    }
}
