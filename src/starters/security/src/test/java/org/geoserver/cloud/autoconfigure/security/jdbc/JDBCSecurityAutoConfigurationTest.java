/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.security.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.jdbc.JDBCSecurityProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class JDBCSecurityAutoConfigurationTest {

    private WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JDBCSecurityAutoConfiguration.class))
            .withBean(GeoServerSecurityManager.class, () -> mock(GeoServerSecurityManager.class));

    @Test
    void testExpectedBeans() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(JDBCSecurityProvider.class)
                .getBean(JDBCSecurityConfigProperties.class)
                .hasFieldOrPropertyWithValue("jdbc", true));
    }

    @Test
    void testDisabled() {
        runner.withPropertyValues("geoserver.security.jdbc=false").run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean(JDBCSecurityProvider.class)
                .doesNotHaveBean(JDBCSecurityConfigProperties.class));
    }
}
