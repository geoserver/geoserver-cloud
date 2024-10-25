/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.security.ldap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.cloud.autoconfigure.security.jdbc.JDBCSecurityConfigProperties;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.ldap.LDAPSecurityProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class LDAPSecurityAutoConfigurationTest {

    private WebApplicationContextRunner runner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(LDAPSecurityAutoConfiguration.class))
                    .withBean(
                            GeoServerSecurityManager.class,
                            () -> mock(GeoServerSecurityManager.class));

    @Test
    void testExpectedBeans() {
        runner.run(
                context ->
                        assertThat(context)
                                .hasNotFailed()
                                .hasSingleBean(LDAPSecurityProvider.class)
                                .getBean(LDAPSecurityConfigProperties.class)
                                .hasFieldOrPropertyWithValue("ldap", true));
    }

    @Test
    void testDisabled() {
        runner.withPropertyValues("geoserver.security.ldap=false")
                .run(
                        context ->
                                assertThat(context)
                                        .hasNotFailed()
                                        .doesNotHaveBean(LDAPSecurityProvider.class)
                                        .doesNotHaveBean(JDBCSecurityConfigProperties.class));
    }
}
