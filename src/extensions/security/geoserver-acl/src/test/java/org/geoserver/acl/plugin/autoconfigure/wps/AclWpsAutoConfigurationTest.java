/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.autoconfigure.wps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.acl.plugin.accessmanager.wps.ChainStatusHolder;
import org.geoserver.acl.plugin.config.wps.WPSResourceManagerClassCondition;
import org.geoserver.acl.plugin.wps.DefaultExecutionIdRetriever;
import org.geoserver.acl.plugin.wps.WPSProcessListener;
import org.geoserver.wps.resource.WPSResourceManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** {@link AclWpsAutoConfiguration} tests */
class AclWpsAutoConfigurationTest {

    private ApplicationContextRunner runner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(AclWpsAutoConfiguration.class));

    @Test
    void testEnabledWhenAllConditionsMatch() {
        runner.withBean("wpsResourceManager", WPSResourceManager.class, () -> mock(WPSResourceManager.class))
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(ChainStatusHolder.class)
                            .hasSingleBean(DefaultExecutionIdRetriever.class)
                            .hasSingleBean(WPSProcessListener.class);
                });
    }

    @Test
    void testConditionalOnAclEnabled() {
        runner.withBean("wpsResourceManager", WPSResourceManager.class, () -> mock(WPSResourceManager.class))
                .withPropertyValues("geoserver.acl.enabled=false")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .doesNotHaveBean(ChainStatusHolder.class)
                            .doesNotHaveBean(DefaultExecutionIdRetriever.class)
                            .doesNotHaveBean(WPSProcessListener.class);
                });
        runner.withPropertyValues("geoserver.acl.enabled=true")
                .withBean("wpsResourceManager", WPSResourceManager.class, () -> mock(WPSResourceManager.class))
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(ChainStatusHolder.class)
                            .hasSingleBean(DefaultExecutionIdRetriever.class)
                            .hasSingleBean(WPSProcessListener.class);
                });
    }

    @Test
    void testConditionalOnWPSResourceManager() {
        runner.run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .doesNotHaveBean(ChainStatusHolder.class)
                    .doesNotHaveBean(DefaultExecutionIdRetriever.class)
                    .doesNotHaveBean(WPSProcessListener.class);
        });
    }

    @Test
    void testWPSResourceManagerClassCondition() {

        FilteredClassLoader classLoader = new FilteredClassLoader(WPSResourceManager.class);
        WPSResourceManagerClassCondition.classLoader(classLoader);
        try {
            runner
                    // bypass the @ConditionalOnBean(name="wpsResourceManager") in the
                    // Autoconfiguration to hit the plain spring condition in
                    // AclWpsIntegrationConfiguration
                    .withBean("wpsResourceManager", Object.class, () -> new Object())
                    .withClassLoader(classLoader)
                    .run(context -> {
                        assertThat(context)
                                .hasNotFailed()
                                .doesNotHaveBean(ChainStatusHolder.class)
                                .doesNotHaveBean(DefaultExecutionIdRetriever.class)
                                .doesNotHaveBean(WPSProcessListener.class);
                    });
        } finally {
            WPSResourceManagerClassCondition.classLoader(null);
        }
    }
}
