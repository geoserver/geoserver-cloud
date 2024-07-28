/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.geoserver.cloud.security.EnvironmentAdminAuthenticationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.io.File;

/**
 * {@link EnableAutoConfiguration @EnableAutoConfiguration} tests for {@link
 * GeoServerSecurityAutoConfiguration}'s {@link EnvironmentAdminAuthenticationProvider}
 *
 * @since 1.0
 */
class EnvironmentAdminAuthenticationProviderTest {

    @TempDir File tempDir;

    private ApplicationContextRunner runner;

    @BeforeEach
    void setUp() {
        runner = GeoServerSecurityAutoConfigurationTest.createContextRunner(tempDir);
    }

    @Test
    @DisplayName(
            "When username is set and password is not set Then FAILS to load the application context")
    void fails_if_password_not_set() {
        runner.withPropertyValues(
                        // USERNAME SET
                        "geoserver.admin.username=myAdmin",
                        // PASSWORD NOT SET
                        "geoserver.admin.password=")
                .run(
                        context -> {
                            assertThat(context)
                                    .hasFailed()
                                    .getFailure()
                                    .hasMessageContaining(
                                            "password not provided through config property geoserver.admin.password");
                        });
    }

    @Test
    @DisplayName(
            "When password is set and username is not set Then FAILS to load the application context")
    void fails_if_username_not_set() {
        runner.withPropertyValues(
                        // USERNAME NOT SET
                        "geoserver.admin.username=",
                        // PASSWORD SET
                        "geoserver.admin.password=s3cr3t")
                .run(
                        context -> {
                            assertThat(context)
                                    .hasFailed()
                                    .getFailure()
                                    .hasMessageContaining(
                                            "admin username not provided through config property geoserver.admin.username");
                        });
    }

    // success case with default admin user
    @Test
    @DisplayName(
            "When using default admin username and password matches Then returns an authenticated token")
    void default_admin_and_password_matches() {
        runner.withPropertyValues(
                        "geoserver.admin.username=admin", "geoserver.admin.password=s3cr3t")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            Authentication token = userNamePasswordToken("admin", "s3cr3t");
                            Authentication authenticated =
                                    envAuthProvider(context).authenticate(token);
                            assertThat(authenticated)
                                    .isInstanceOf(UsernamePasswordAuthenticationToken.class)
                                    .isNotSameAs(token);

                            assertThat(authenticated)
                                    .as("should return a fully authenticated token")
                                    .hasFieldOrPropertyWithValue("authenticated", true);

                            assertThat(authenticated)
                                    .as("should have the expected admin roles")
                                    .hasFieldOrPropertyWithValue(
                                            "authorities",
                                            EnvironmentAdminAuthenticationProvider.adminRoles());
                        });
    }

    // success case with a non default admin username
    @Test
    @DisplayName(
            "When NOT using default admin username and password matches Then returns an authenticated token")
    void non_default_admin_and_password_matches() {
        runner.withPropertyValues(
                        "geoserver.admin.username=JohnTheAdmin", "geoserver.admin.password=s3cr3t")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            Authentication token = userNamePasswordToken("JohnTheAdmin", "s3cr3t");
                            Authentication authenticated =
                                    envAuthProvider(context).authenticate(token);
                            assertThat(authenticated)
                                    .isInstanceOf(UsernamePasswordAuthenticationToken.class)
                                    .isNotSameAs(token);

                            assertThat(authenticated)
                                    .as("should return a fully authenticated token")
                                    .hasFieldOrPropertyWithValue("authenticated", true);

                            assertThat(authenticated)
                                    .as("should have the expected admin roles")
                                    .hasFieldOrPropertyWithValue(
                                            "authorities",
                                            EnvironmentAdminAuthenticationProvider.adminRoles());
                        });
    }

    @Test
    @DisplayName(
            "When using default admin username and password does not match Then breaks the auth chain")
    void default_admin_bad_credentials() {
        runner.withPropertyValues(
                        "geoserver.admin.username=admin", "geoserver.admin.password=s3cr3t")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            Authentication token = userNamePasswordToken("admin", "badPWD");
                            EnvironmentAdminAuthenticationProvider envAuthProvider =
                                    envAuthProvider(context);

                            assertThrows(
                                    InternalAuthenticationServiceException.class,
                                    () -> envAuthProvider.authenticate(token));
                        });
    }

    @Test
    @DisplayName("When not using admin as username Then the default admin username cannot be used")
    void if_configured_hides_default_admin() {
        runner.withPropertyValues(
                        "geoserver.admin.username=MyCustomAdmin", "geoserver.admin.password=s3cr3t")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();

                            Authentication defaultAdminCredentials =
                                    userNamePasswordToken("admin", "geoserver");
                            EnvironmentAdminAuthenticationProvider authProvider =
                                    envAuthProvider(context);
                            assertThrows(
                                    InternalAuthenticationServiceException.class,
                                    () -> authProvider.authenticate(defaultAdminCredentials),
                                    "The admin user should be disabled if geoserver.admin.username is set to another value");
                        });
    }

    protected UsernamePasswordAuthenticationToken userNamePasswordToken(
            String principal, String credentials) {
        return new UsernamePasswordAuthenticationToken(principal, credentials);
    }

    protected EnvironmentAdminAuthenticationProvider envAuthProvider(
            AssertableApplicationContext context) {
        return context.getBean(EnvironmentAdminAuthenticationProvider.class);
    }
}
