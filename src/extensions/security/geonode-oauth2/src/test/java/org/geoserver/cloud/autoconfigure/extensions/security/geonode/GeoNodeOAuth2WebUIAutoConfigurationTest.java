/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.geonode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;
import org.geoserver.web.GeoServerApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;

/**
 * Test class for {@link GeoNodeOAuth2WebUIAutoConfiguration}.
 *
 * <p>
 * This class tests the auto-configuration of GeoNode OAuth2 web UI components, verifying:
 * <ul>
 *   <li>Conditional activation based on GeoNode OAuth2 extension being enabled/disabled</li>
 *   <li>Conditional activation based on GeoServer web UI components being present</li>
 *   <li>Proper bean registration under different conditions</li>
 * </ul>
 *
 * <p>
 * The tests use Spring Boot's ApplicationContextRunner to simulate different
 * application contexts with various configurations and class availability.
 *
 * @since 2.27.0
 */
class GeoNodeOAuth2WebUIAutoConfigurationTest {

    /** Application context runner for testing different configurations */
    private ApplicationContextRunner runner;

    /**
     * Sets up the test environment before each test.
     *
     * <p>
     * Creates a mock GeoServerSecurityManager and configures an ApplicationContextRunner
     * with the necessary beans and auto-configurations for testing.
     */
    @BeforeEach
    void beforeEach() {
        GeoServerSecurityManager authenticationManager = mock(GeoServerSecurityManager.class);
        ApplicationContext appc = mock(ApplicationContext.class);
        when(authenticationManager.getApplicationContext()).thenReturn(appc);

        runner = new ApplicationContextRunner()
                .withBean("authenticationManager", GeoServerSecurityManager.class, () -> authenticationManager)
                .withConfiguration(AutoConfigurations.of(
                        GeoNodeOAuth2AutoConfiguration.class, GeoNodeOAuth2WebUIAutoConfiguration.class));
    }

    /**
     * Tests the conditional activation based on the GeoNode OAuth2 extension being enabled or disabled.
     *
     * <p>
     * Verifies that:
     * <ul>
     *   <li>With default settings (enabled=true), the required beans are present</li>
     *   <li>With explicit enabled=true, the required beans are present</li>
     *   <li>With explicit enabled=false, no beans are registered</li>
     * </ul>
     *
     * <p>
     * The test uses a FilteredClassLoader to exclude web UI classes, ensuring
     * only the core OAuth2 beans are tested without UI components.
     */
    @Test
    void testConditionalOnGeoNodeOAuth2() {
        FilteredClassLoader filteredClassLoader =
                new FilteredClassLoader(GeoServerApplication.class, AuthenticationFilterPanelInfo.class);

        runner.withClassLoader(filteredClassLoader).run(this::assertEnabled);

        runner.withClassLoader(filteredClassLoader)
                .withPropertyValues("geoserver.extension.security.geonode-oauth2.enabled=true")
                .run(this::assertEnabled);

        runner.withClassLoader(filteredClassLoader)
                .withPropertyValues("geoserver.extension.security.geonode-oauth2.enabled=false")
                .run(this::assertDisabled);
    }

    /**
     * Tests the conditional activation based on GeoServer web UI components being present.
     *
     * <p>
     * Verifies that:
     * <ul>
     *   <li>Without web UI classes, no UI-specific beans are registered</li>
     *   <li>With web UI classes, UI-specific beans are properly registered</li>
     * </ul>
     *
     * <p>
     * This test ensures that the web UI components are only activated when
     * the GeoServer web application classes are available in the classpath.
     */
    @Test
    void testConditionalOnGeoServerWebUI() {
        FilteredClassLoader filteredClassLoader =
                new FilteredClassLoader(GeoServerApplication.class, AuthenticationFilterPanelInfo.class);

        runner.withClassLoader(filteredClassLoader).run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean("geoNodeOAuth2AuthPanelInfo")
                .doesNotHaveBean("geonodeFormLoginButton"));

        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasBean("geoNodeOAuth2AuthPanelInfo")
                .hasBean("geonodeFormLoginButton"));
    }

    /**
     * Helper method to assert that the GeoNode OAuth2 extension is properly enabled.
     *
     * <p>
     * Verifies that the application context contains all the necessary beans for the
     * GeoNode OAuth2 extension, but not the web UI components.
     *
     * @param context The Spring application context to check
     */
    private void assertEnabled(AssertableApplicationContext context) {
        assertThat(context)
                .hasNotFailed()
                .hasBean("geonodeOauth2Extension")
                .hasSingleBean(org.geoserver.security.oauth2.services.GeoNodeTokenServices.class)
                .hasSingleBean(org.geoserver.security.oauth2.GeoNodeOAuth2AuthenticationProvider.class)
                .doesNotHaveBean("geoNodeOAuth2AuthPanelInfo")
                .doesNotHaveBean("geonodeFormLoginButton");
    }

    /**
     * Helper method to assert that the GeoNode OAuth2 extension is properly disabled.
     *
     * <p>
     * Verifies that the application context does not contain any of the beans
     * related to the GeoNode OAuth2 extension.
     *
     * @param context The Spring application context to check
     */
    private void assertDisabled(AssertableApplicationContext context) {
        assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean("geonodeOauth2Extension")
                .doesNotHaveBean(org.geoserver.security.oauth2.services.GeoNodeTokenServices.class)
                .doesNotHaveBean(org.geoserver.security.oauth2.GeoNodeOAuth2AuthenticationProvider.class);
    }
}
