/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.authzn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.platform.ModuleStatus;
import org.geoserver.security.AuthenticationKeyMangler;
import org.geoserver.security.GeoServerAuthenticationKeyProvider;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.PropertyAuthenticationKeyMapper;
import org.geoserver.security.UserPropertyAuthenticationKeyMapper;
import org.geoserver.security.WebServiceAuthenticationKeyMapper;
import org.geoserver.security.WebServiceBodyResponseSecurityProvider;
import org.geoserver.security.web.AuthenticationKeyFilterPanelInfo;
import org.geoserver.security.web.GeoServerRestRoleServicePanelInfo;
import org.geoserver.security.web.WebServiceBodyResponseUserGroupServicePanelInfo;
import org.geoserver.security.web.auth.AuthenticationFilterPanel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * Test suite for {@link AuthKeyAutoConfiguration}
 *
 * @since 1.0
 */
class AuthKeyAutoConfigurationTest {

    private WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(AuthKeyAutoConfiguration.class));

    @BeforeEach
    void mockBeanDependencies() {
        GeoServerSecurityManager mockSM = mock(GeoServerSecurityManager.class);
        contextRunner = contextRunner.withBean(GeoServerSecurityManager.class, () -> mockSM);
    }

    @Test
    void testModuleStatus_disabled_by_default() {
        contextRunner
                .run(context -> assertThat(context).hasBean("authKeyExtension"))
                .run(
                        context ->
                                assertThat(context)
                                        .getBean("authKeyExtension", ModuleStatus.class)
                                        .hasFieldOrPropertyWithValue("enabled", false));
    }

    @Test
    void testModuleStatus_enabled() {
        contextRunner
                .withPropertyValues("geoserver.security.authkey=true")
                .run(context -> assertThat(context).hasBean("authKeyExtension"))
                .run(
                        context ->
                                assertThat(context)
                                        .getBean("authKeyExtension", ModuleStatus.class)
                                        .hasFieldOrPropertyWithValue("enabled", true));
    }

    @Test
    void testModuleStatus_disabled() {
        contextRunner
                .withPropertyValues("geoserver.security.authkey=false")
                .run(context -> assertThat(context).hasBean("authKeyExtension"))
                .run(
                        context ->
                                assertThat(context)
                                        .getBean("authKeyExtension", ModuleStatus.class)
                                        .hasFieldOrPropertyWithValue("enabled", false));
    }

    @Test
    void testGeoServerAuthenticationKeyProvider_enabled_no_GsWebSecCore_InClasspath() {
        contextRunner
                .withPropertyValues("geoserver.security.authkey=true")
                // AuthenticationFilterPanel is from gs-web-sec-core, used as @ConditionalOnClass to
                // enabled the web-ui components
                .withClassLoader(new FilteredClassLoader(AuthenticationFilterPanel.class))
                .run(
                        ctx -> {
                            assertThat(ctx).doesNotHaveBean(AuthenticationKeyFilterPanelInfo.class);
                            assertThat(ctx)
                                    .doesNotHaveBean(GeoServerRestRoleServicePanelInfo.class);
                            assertThat(ctx)
                                    .doesNotHaveBean(
                                            WebServiceBodyResponseUserGroupServicePanelInfo.class);
                        });
    }

    @Test
    void testGeoServerAuthenticationKeyProvider_enabled() {
        contextRunner
                .withPropertyValues("geoserver.security.authkey=true")
                .run(
                        ctx -> {
                            assertThat(ctx).hasSingleBean(GeoServerAuthenticationKeyProvider.class);
                            assertThat(ctx).hasSingleBean(AuthenticationKeyMangler.class);
                            assertThat(ctx).hasSingleBean(PropertyAuthenticationKeyMapper.class);
                            assertThat(ctx)
                                    .hasSingleBean(UserPropertyAuthenticationKeyMapper.class);
                            assertThat(ctx).hasSingleBean(WebServiceAuthenticationKeyMapper.class);
                            assertThat(ctx).hasSingleBean(GeoServerAuthenticationKeyProvider.class);
                            assertThat(ctx)
                                    .hasSingleBean(WebServiceBodyResponseSecurityProvider.class);

                            assertThat(ctx).hasSingleBean(AuthenticationKeyFilterPanelInfo.class);
                            assertThat(ctx)
                                    .hasSingleBean(
                                            WebServiceBodyResponseUserGroupServicePanelInfo.class);
                            assertThat(ctx).hasSingleBean(GeoServerRestRoleServicePanelInfo.class);
                        });
    }
}
