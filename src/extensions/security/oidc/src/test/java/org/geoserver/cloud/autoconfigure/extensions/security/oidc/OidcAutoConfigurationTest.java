/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.geoserver.platform.ModuleStatus;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationFilter;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationProvider;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;
import org.geoserver.web.security.oauth2.login.OAuth2LoginAuthProviderPanelInfo;
import org.geoserver.web.security.oauth2.login.OAuth2LoginButtonManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * Slice tests for {@link OidcAutoConfiguration}.
 *
 * @since 3.0.3
 */
class OidcAutoConfigurationTest {

    private WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(OidcAutoConfiguration.class));

    @BeforeEach
    void mockBeanDependencies() {
        var mockGeoServer = mock(org.geoserver.config.GeoServer.class);
        // GeoServer's XML aliases the SecurityManager as "authenticationManager"; the transpiled
        // oauth2LoginAuthenticationProvider depends on that exact qualifier.
        contextRunner = contextRunner
                .withBean("geoServer", org.geoserver.config.GeoServer.class, () -> mockGeoServer)
                .withBean(
                        "authenticationManager",
                        GeoServerSecurityManager.class,
                        () -> mock(GeoServerSecurityManager.class));
    }

    @Test
    void testEnabledByDefault() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(GeoServerOAuth2LoginAuthenticationProvider.class);
            assertThat(ctx).hasBean("openIdConnectCoreExtension");
        });
    }

    @Test
    void testDisabledByFlag() {
        contextRunner
                .withPropertyValues("geoserver.extension.security.oidc.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(GeoServerOAuth2LoginAuthenticationProvider.class);
                    assertThat(ctx).doesNotHaveBean("openIdConnectCoreExtension");
                });
    }

    @Test
    void testClasspathAbsent_extensionInert() {
        contextRunner
                .withPropertyValues("geoserver.extension.security.oidc.enabled=true")
                .withClassLoader(new FilteredClassLoader(GeoServerOAuth2LoginAuthenticationFilter.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(GeoServerOAuth2LoginAuthenticationProvider.class));
    }

    @Test
    void testEnabled_coreBeansRegistered() {
        contextRunner
                .withPropertyValues("geoserver.extension.security.oidc.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(GeoServerOAuth2LoginAuthenticationProvider.class);
                    assertThat(ctx)
                            .getBean("openIdConnectCoreExtension", ModuleStatus.class)
                            .hasFieldOrPropertyWithValue("module", "gs-sec-oidc-core");
                });
    }

    @Test
    void testEnabled_webUiBeansAbsentWithoutWebClass() {
        contextRunner
                .withPropertyValues("geoserver.extension.security.oidc.enabled=true")
                // Filter the gs-web-sec-core anchor class used by the WebUI inner config
                .withClassLoader(new FilteredClassLoader(AuthenticationFilterPanelInfo.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(GeoServerOAuth2LoginAuthenticationProvider.class);
                    assertThat(ctx).doesNotHaveBean(OAuth2LoginAuthProviderPanelInfo.class);
                    assertThat(ctx).doesNotHaveBean(OAuth2LoginButtonManager.class);
                });
    }

    @Test
    void testEnabled_webUiBeansPresent() {
        contextRunner
                .withPropertyValues(
                        "geoserver.service.webui.enabled=true", "geoserver.extension.security.oidc.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(GeoServerOAuth2LoginAuthenticationProvider.class);
                    assertThat(ctx).hasSingleBean(OAuth2LoginAuthProviderPanelInfo.class);
                    assertThat(ctx).hasSingleBean(OAuth2LoginButtonManager.class);
                });
    }

    // the web classes are on the classpath: only the service condition keeps the beans out
    @Test
    void testEnabled_noWebUiBeansInWms() {
        contextRunner
                .withPropertyValues("geoserver.services=wms", "geoserver.extension.security.oidc.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(GeoServerOAuth2LoginAuthenticationProvider.class);
                    assertThat(ctx).doesNotHaveBean(OAuth2LoginAuthProviderPanelInfo.class);
                    assertThat(ctx).doesNotHaveBean(OAuth2LoginButtonManager.class);
                });
    }
}
