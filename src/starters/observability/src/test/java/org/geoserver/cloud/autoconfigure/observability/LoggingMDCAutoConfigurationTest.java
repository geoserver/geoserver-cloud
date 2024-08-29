/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.cloud.observability.logging.config.MDCConfigProperties;
import org.geoserver.cloud.observability.logging.ows.MDCDispatcherCallback;
import org.geoserver.cloud.observability.logging.servlet.HttpRequestMdcFilter;
import org.geoserver.cloud.observability.logging.servlet.MDCCleaningFilter;
import org.geoserver.cloud.observability.logging.servlet.SpringEnvironmentMdcFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.core.Authentication;

class LoggingMDCAutoConfigurationTest {

    private WebApplicationContextRunner runner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(LoggingMDCAutoConfiguration.class));

    @Test
    void testDefaultBeans() {
        runner.run(
                context ->
                        assertThat(context)
                                .hasNotFailed()
                                .hasSingleBean(MDCConfigProperties.class)
                                .hasSingleBean(MDCDispatcherCallback.class)
                                .hasSingleBean(MDCCleaningFilter.class)
                                .hasSingleBean(HttpRequestMdcFilter.class)
                                .hasSingleBean(SpringEnvironmentMdcFilter.class)
                                .hasBean("mdcAuthenticationPropertiesServletFilter"));
    }

    @Test
    void testMDCConfigProperties() {
        MDCConfigProperties defaults = new MDCConfigProperties();

        runner.withPropertyValues(
                        "logging.mdc.include.user=%s".formatted(!defaults.isUser()),
                        "logging.mdc.include.roles=%s".formatted(!defaults.isRoles()),
                        "logging.mdc.include.ows=%s".formatted(!defaults.isOws()))
                .run(
                        context ->
                                assertThat(context)
                                        .getBean(MDCConfigProperties.class)
                                        .hasFieldOrPropertyWithValue("user", !defaults.isUser())
                                        .hasFieldOrPropertyWithValue("roles", !defaults.isRoles())
                                        .hasFieldOrPropertyWithValue("ows", !defaults.isOws()));
    }

    @Test
    void conditionalOnGeoServerDispatcher() {
        runner.withClassLoader(new FilteredClassLoader(org.geoserver.ows.Dispatcher.class))
                .run(
                        context ->
                                assertThat(context)
                                        .hasNotFailed()
                                        .doesNotHaveBean(MDCDispatcherCallback.class));
    }

    @Test
    void conditionalOnServletWebApplication() {
        ReactiveWebApplicationContextRunner reactiveAppRunner =
                new ReactiveWebApplicationContextRunner()
                        .withConfiguration(
                                AutoConfigurations.of(LoggingMDCAutoConfiguration.class));
        reactiveAppRunner.run(
                context ->
                        assertThat(context)
                                .hasNotFailed()
                                .doesNotHaveBean(MDCConfigProperties.class)
                                .doesNotHaveBean(MDCDispatcherCallback.class)
                                .doesNotHaveBean("mdcCleaningServletFilter"));
    }

    @Test
    void authenticationFilterConditionalOnAuthenticationClass() {
        runner.withClassLoader(new FilteredClassLoader(Authentication.class))
                .run(
                        context ->
                                assertThat(context)
                                        .hasNotFailed()
                                        .doesNotHaveBean(
                                                "mdcAuthenticationPropertiesServletFilter"));
    }
}
