/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.security.environmentadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.geoserver.security.GeoServerSecurityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * Test suite for {@link EnvironmentAdminAutoConfiguration}
 *
 * @since 2.27.0
 */
class EnvironmentAdminAutoConfigurationTest {

    private ApplicationContextRunner contextRunner;

    @BeforeEach
    void setUp() {
        contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EnvironmentAdminAutoConfiguration.class))
                .withBean(GeoServerSecurityManager.class, () -> mock(GeoServerSecurityManager.class));
    }

    @Test
    @DisplayName("When extension is disabled then no beans are created")
    void testDisabled() {
        contextRunner
                .withPropertyValues("geoserver.extension.security.environment-admin.enabled=false")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(EnvironmentAdminAuthenticationProvider.class));
    }

    @Test
    @DisplayName("When extension is enabled but username/password not set then provider is disabled")
    void testEnabled_NoCredentials() {
        contextRunner
                .withPropertyValues("geoserver.extension.security.environment-admin.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(EnvironmentAdminAuthenticationProvider.class);

                    // Provider should be created but disabled
                    EnvironmentAdminAuthenticationProvider provider =
                            context.getBean(EnvironmentAdminAuthenticationProvider.class);

                    // Create a test authentication token
                    Authentication token = new UsernamePasswordAuthenticationToken("testuser", "testpassword");

                    // Provider should return null (not authenticate) when disabled
                    assertThat(provider.authenticate(token)).isNull();
                });
    }

    @Test
    @DisplayName("When only username is set, application context should fail to load")
    void testUsernameOnly() {
        contextRunner
                .withPropertyValues(
                        "geoserver.extension.security.environment-admin.enabled=true",
                        "geoserver.admin.username=admin",
                        "geoserver.admin.password=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("password not provided through config property geoserver.admin.password");
                });
    }

    @Test
    @DisplayName("When only password is set, application context should fail to load")
    void testPasswordOnly() {
        contextRunner
                .withPropertyValues(
                        "geoserver.extension.security.environment-admin.enabled=true",
                        "geoserver.admin.username=",
                        "geoserver.admin.password=s3cr3t")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("admin username not provided through config property geoserver.admin.username");
                });
    }

    @Test
    @DisplayName("When enabled with valid credentials, provider should authenticate matching credentials")
    void testSuccessfulAuthentication() {
        contextRunner
                .withPropertyValues(
                        "geoserver.extension.security.environment-admin.enabled=true",
                        "geoserver.admin.username=admin",
                        "geoserver.admin.password=s3cr3t")
                .run(context -> {
                    assertThat(context).hasSingleBean(EnvironmentAdminAuthenticationProvider.class);

                    EnvironmentAdminAuthenticationProvider provider =
                            context.getBean(EnvironmentAdminAuthenticationProvider.class);

                    // Valid credentials
                    Authentication token = new UsernamePasswordAuthenticationToken("admin", "s3cr3t");
                    Authentication result = provider.authenticate(token);

                    assertThat(result).isNotNull();
                    assertThat(result.isAuthenticated()).isTrue();
                    assertThat(result.getAuthorities()).isEqualTo(EnvironmentAdminAuthenticationProvider.adminRoles());
                });
    }

    @Test
    @DisplayName("When enabled with valid credentials, provider should reject mismatched credentials")
    void testFailedAuthentication() {
        contextRunner
                .withPropertyValues(
                        "geoserver.extension.security.environment-admin.enabled=true",
                        "geoserver.admin.username=admin",
                        "geoserver.admin.password=s3cr3t")
                .run(context -> {
                    assertThat(context).hasSingleBean(EnvironmentAdminAuthenticationProvider.class);

                    EnvironmentAdminAuthenticationProvider provider =
                            context.getBean(EnvironmentAdminAuthenticationProvider.class);

                    // Invalid password
                    Authentication token = new UsernamePasswordAuthenticationToken("admin", "wrong");

                    assertThrows(InternalAuthenticationServiceException.class, () -> provider.authenticate(token));
                });
    }

    @Test
    @DisplayName("When custom admin username is set, default admin is disabled")
    void testCustomAdminDisablesDefaultAdmin() {
        contextRunner
                .withPropertyValues(
                        "geoserver.extension.security.environment-admin.enabled=true",
                        "geoserver.admin.username=customadmin",
                        "geoserver.admin.password=s3cr3t")
                .run(context -> {
                    assertThat(context).hasSingleBean(EnvironmentAdminAuthenticationProvider.class);

                    EnvironmentAdminAuthenticationProvider provider =
                            context.getBean(EnvironmentAdminAuthenticationProvider.class);

                    // Try to authenticate with default admin username
                    Authentication token = new UsernamePasswordAuthenticationToken("admin", "any");

                    assertThrows(
                            InternalAuthenticationServiceException.class,
                            () -> provider.authenticate(token),
                            "Default admin user should be disabled");
                });
    }
}
