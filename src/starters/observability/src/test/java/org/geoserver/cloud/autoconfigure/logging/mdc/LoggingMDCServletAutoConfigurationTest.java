/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.logging.mdc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.geoserver.cloud.logging.mdc.config.AuthenticationMdcConfigProperties;
import org.geoserver.cloud.logging.mdc.config.GeoServerMdcConfigProperties;
import org.geoserver.cloud.logging.mdc.config.HttpRequestMdcConfigProperties;
import org.geoserver.cloud.logging.mdc.config.SpringEnvironmentMdcConfigProperties;
import org.geoserver.cloud.logging.mdc.ows.OWSMdcDispatcherCallback;
import org.geoserver.cloud.logging.mdc.servlet.HttpRequestMdcFilter;
import org.geoserver.cloud.logging.mdc.servlet.MDCCleaningFilter;
import org.geoserver.cloud.logging.mdc.servlet.SpringEnvironmentMdcFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.core.Authentication;

class LoggingMDCServletAutoConfigurationTest {

    private WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    LoggingMDCServletAutoConfiguration.class, GeoServerDispatcherMDCAutoConfiguration.class));

    @Test
    void testDefaultBeans() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(AuthenticationMdcConfigProperties.class)
                .hasSingleBean(HttpRequestMdcConfigProperties.class)
                .hasSingleBean(SpringEnvironmentMdcConfigProperties.class)
                .hasSingleBean(GeoServerMdcConfigProperties.class)
                .hasSingleBean(OWSMdcDispatcherCallback.class)
                .hasSingleBean(MDCCleaningFilter.class)
                .hasSingleBean(HttpRequestMdcFilter.class)
                .hasSingleBean(SpringEnvironmentMdcFilter.class)
                .hasBean("mdcAuthenticationPropertiesServletFilter"));
    }

    @Test
    void testDefaultMDCConfigProperties() {
        runner.run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(AuthenticationMdcConfigProperties.class)
                    .hasSingleBean(HttpRequestMdcConfigProperties.class)
                    .hasSingleBean(SpringEnvironmentMdcConfigProperties.class)
                    .hasSingleBean(GeoServerMdcConfigProperties.class)
                    .hasSingleBean(OWSMdcDispatcherCallback.class);

            AuthenticationMdcConfigProperties user = context.getBean(AuthenticationMdcConfigProperties.class);
            HttpRequestMdcConfigProperties http = context.getBean(HttpRequestMdcConfigProperties.class);
            SpringEnvironmentMdcConfigProperties app = context.getBean(SpringEnvironmentMdcConfigProperties.class);
            GeoServerMdcConfigProperties gs = context.getBean(GeoServerMdcConfigProperties.class);

            assertThat(user).hasFieldOrPropertyWithValue("id", false).hasFieldOrPropertyWithValue("roles", false);

            assertThat(http)
                    .hasFieldOrPropertyWithValue("id", true)
                    .hasFieldOrPropertyWithValue("remoteAddr", false)
                    .hasFieldOrPropertyWithValue("remoteHost", false)
                    .hasFieldOrPropertyWithValue("method", true)
                    .hasFieldOrPropertyWithValue("url", true)
                    .hasFieldOrPropertyWithValue("parameters", false)
                    .hasFieldOrPropertyWithValue("queryString", false)
                    .hasFieldOrPropertyWithValue("sessionId", false)
                    .hasFieldOrPropertyWithValue("cookies", false)
                    .hasFieldOrPropertyWithValue("headers", false);

            assertThat(http.getHeadersPattern().pattern()).isEqualTo(".*");

            assertThat(app)
                    .hasFieldOrPropertyWithValue("name", true)
                    .hasFieldOrPropertyWithValue("version", false)
                    .hasFieldOrPropertyWithValue("instanceId", false)
                    .hasFieldOrPropertyWithValue("activeProfiles", false)
                    .hasFieldOrPropertyWithValue(
                            "instanceIdProperties", List.of("info.instance-id", "spring.application.instance_id"));

            assertThat(gs)
                    .hasFieldOrPropertyWithValue("ows.serviceName", true)
                    .hasFieldOrPropertyWithValue("ows.serviceVersion", true)
                    .hasFieldOrPropertyWithValue("ows.serviceFormat", true)
                    .hasFieldOrPropertyWithValue("ows.operationName", true);
        });
    }

    @Test
    void testMDCConfigProperties() {
        runner.withPropertyValues(
                        "logging.mdc.include.user.id=true",
                        "logging.mdc.include.user.roles=true",
                        "logging.mdc.include.application.version=true",
                        "logging.mdc.include.application.instance-id=true",
                        "logging.mdc.include.http.headers=true",
                        "logging.mdc.include.geoserver.ows.service-name=false")
                .run(context -> {
                    assertThat(context)
                            .getBean(AuthenticationMdcConfigProperties.class)
                            .hasFieldOrPropertyWithValue("id", true)
                            .hasFieldOrPropertyWithValue("roles", true);
                    assertThat(context)
                            .getBean(SpringEnvironmentMdcConfigProperties.class)
                            .hasFieldOrPropertyWithValue("version", true)
                            .hasFieldOrPropertyWithValue("instanceId", true);
                    assertThat(context)
                            .getBean(HttpRequestMdcConfigProperties.class)
                            .hasFieldOrPropertyWithValue("headers", true);
                    assertThat(context)
                            .getBean(GeoServerMdcConfigProperties.class)
                            .hasFieldOrPropertyWithValue("ows.serviceName", false);
                });
    }

    @Test
    void conditionalOnGeoServerDispatcherCallback() {
        runner.withClassLoader(new FilteredClassLoader(org.geoserver.ows.DispatcherCallback.class))
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(OWSMdcDispatcherCallback.class)
                        .doesNotHaveBean(GeoServerMdcConfigProperties.class));
    }

    @Test
    void conditionalOnServletWebApplication() {
        ReactiveWebApplicationContextRunner reactiveAppRunner = new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        LoggingMDCServletAutoConfiguration.class, GeoServerDispatcherMDCAutoConfiguration.class));
        reactiveAppRunner.run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean(AuthenticationMdcConfigProperties.class)
                .doesNotHaveBean(HttpRequestMdcConfigProperties.class)
                .doesNotHaveBean(SpringEnvironmentMdcConfigProperties.class)
                .doesNotHaveBean(GeoServerMdcConfigProperties.class)
                .doesNotHaveBean(OWSMdcDispatcherCallback.class)
                .doesNotHaveBean("mdcCleaningServletFilter"));
    }

    @Test
    void authenticationFilterConditionalOnAuthenticationClass() {
        runner.withClassLoader(new FilteredClassLoader(Authentication.class))
                .run(context ->
                        assertThat(context).hasNotFailed().doesNotHaveBean("mdcAuthenticationPropertiesServletFilter"));
    }
}
