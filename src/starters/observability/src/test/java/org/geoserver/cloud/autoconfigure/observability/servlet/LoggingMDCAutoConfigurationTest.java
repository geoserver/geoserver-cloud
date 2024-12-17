/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.observability.servlet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.geoserver.cloud.autoconfigure.logging.mdc.LoggingMDCServletAutoConfiguration;
import org.geoserver.cloud.logging.mdc.config.MDCConfigProperties;
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

class LoggingMDCAutoConfigurationTest {

    private WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LoggingMDCServletAutoConfiguration.class));

    @Test
    void testDefaultBeans() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(MDCConfigProperties.class)
                .hasSingleBean(OWSMdcDispatcherCallback.class)
                .hasSingleBean(MDCCleaningFilter.class)
                .hasSingleBean(HttpRequestMdcFilter.class)
                .hasSingleBean(SpringEnvironmentMdcFilter.class)
                .hasBean("mdcAuthenticationPropertiesServletFilter"));
    }

    @Test
    void testDefaultMDCConfigProperties() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(MDCConfigProperties.class);
            MDCConfigProperties defaults = context.getBean(MDCConfigProperties.class);

            assertThat(defaults.getUser())
                    .hasFieldOrPropertyWithValue("id", false)
                    .hasFieldOrPropertyWithValue("roles", false);

            assertThat(defaults.getHttp())
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

            assertThat(defaults.getHttp().getHeadersPattern().pattern()).isEqualTo(".*");

            assertThat(defaults.getApplication())
                    .hasFieldOrPropertyWithValue("name", true)
                    .hasFieldOrPropertyWithValue("version", false)
                    .hasFieldOrPropertyWithValue("instanceId", false)
                    .hasFieldOrPropertyWithValue("activeProfiles", false)
                    .hasFieldOrPropertyWithValue(
                            "instanceIdProperties", List.of("info.instance-id", "spring.application.instance_id"));

            assertThat(defaults.getGeoserver())
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
                .run(context -> assertThat(context)
                        .getBean(MDCConfigProperties.class)
                        .hasFieldOrPropertyWithValue("user.id", true)
                        .hasFieldOrPropertyWithValue("user.roles", true)
                        .hasFieldOrPropertyWithValue("application.version", true)
                        .hasFieldOrPropertyWithValue("application.instanceId", true)
                        .hasFieldOrPropertyWithValue("http.headers", true)
                        .hasFieldOrPropertyWithValue("geoserver.ows.serviceName", false));
    }

    @Test
    void conditionalOnGeoServerDispatcher() {
        runner.withClassLoader(new FilteredClassLoader(org.geoserver.ows.Dispatcher.class))
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(OWSMdcDispatcherCallback.class));
    }

    @Test
    void conditionalOnServletWebApplication() {
        ReactiveWebApplicationContextRunner reactiveAppRunner = new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(LoggingMDCServletAutoConfiguration.class));
        reactiveAppRunner.run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean(MDCConfigProperties.class)
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
