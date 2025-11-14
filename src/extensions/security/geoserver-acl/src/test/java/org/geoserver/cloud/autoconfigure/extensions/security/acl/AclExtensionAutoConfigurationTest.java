/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.config.GeoServer;
import org.geoserver.security.GeoServerSecurityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Test suite for {@link AclExtensionAutoConfiguration}
 *
 * @since 2.27.0.0
 */
class AclExtensionAutoConfigurationTest {

    private ApplicationContextRunner contextRunner;

    @BeforeEach
    void setup() {
        // Create a mock GeoServer instance to satisfy @ConditionalOnGeoServer
        var mockGeoServer = mock(GeoServer.class);

        contextRunner = new ApplicationContextRunner()
                .withBean("geoServer", GeoServer.class, () -> mockGeoServer)
                .withBean(GeoServerSecurityManager.class, () -> mock(GeoServerSecurityManager.class))
                .withConfiguration(AutoConfigurations.of(AclExtensionAutoConfiguration.class));
    }

    @Test
    void testDisabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(AclExtensionAutoConfiguration.class);
            assertThat(context).doesNotHaveBean(AclExtensionConfigurationProperties.class);
        });
    }

    @Test
    void testExplicitlyDisabled() {
        contextRunner
                .withPropertyValues("geoserver.extension.security.acl.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(AclExtensionAutoConfiguration.class);
                });
    }

    @Test
    void testExplicitlyEnabled() {
        contextRunner
                .withPropertyValues("geoserver.extension.security.acl.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AclExtensionAutoConfiguration.class);
                    assertThat(context)
                            .getBean(AclExtensionConfigurationProperties.class)
                            .hasFieldOrPropertyWithValue("enabled", true);
                });
    }
}
