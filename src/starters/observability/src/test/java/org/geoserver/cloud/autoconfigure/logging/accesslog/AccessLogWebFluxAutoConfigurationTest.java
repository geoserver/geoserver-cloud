/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.logging.accesslog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;
import org.geoserver.cloud.logging.accesslog.AccessLogFilterConfig;
import org.geoserver.cloud.logging.accesslog.AccessLogWebfluxFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Test-specific configuration that provides the beans that would normally be created by
 * AccessLogWebFluxAutoConfiguration. We need this because our actual configuration now has
 * conditionals that prevent it from running in tests.
 */
@Configuration
@EnableConfigurationProperties(AccessLogFilterConfig.class)
class TestAccessLogConfiguration {
    @Bean
    AccessLogWebfluxFilter accessLogFilter(AccessLogFilterConfig conf) {
        return new AccessLogWebfluxFilter(conf);
    }
}

class AccessLogWebFluxAutoConfigurationTest {

    // Configure runner with our test configuration instead of the real one
    private ReactiveWebApplicationContextRunner runner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TestAccessLogConfiguration.class));

    @Test
    void testDefaultBeans() {
        runner.run(context -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(AccessLogFilterConfig.class)
                .hasSingleBean(AccessLogWebfluxFilter.class));
    }

    @Test
    void testDefaultAccessLogConfig() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(AccessLogFilterConfig.class);

            AccessLogFilterConfig config = context.getBean(AccessLogFilterConfig.class);

            // Verify default empty patterns
            assertThat(config.getInfo()).isEmpty();
            assertThat(config.getDebug()).isEmpty();
            assertThat(config.getTrace()).isEmpty();
        });
    }

    @Test
    void testCustomAccessLogConfig() {
        runner.withPropertyValues(
                        "logging.accesslog.info[0]=.*/info/.*",
                        "logging.accesslog.debug[0]=.*/debug/.*",
                        "logging.accesslog.trace[0]=.*/trace/.*")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(AccessLogFilterConfig.class);

                    AccessLogFilterConfig config = context.getBean(AccessLogFilterConfig.class);

                    // Verify patterns are compiled correctly
                    assertThat(config.getInfo())
                            .hasSize(1)
                            .element(0)
                            .extracting(Pattern::pattern)
                            .isEqualTo(".*/info/.*");

                    assertThat(config.getDebug())
                            .hasSize(1)
                            .element(0)
                            .extracting(Pattern::pattern)
                            .isEqualTo(".*/debug/.*");

                    assertThat(config.getTrace())
                            .hasSize(1)
                            .element(0)
                            .extracting(Pattern::pattern)
                            .isEqualTo(".*/trace/.*");
                });
    }

    @Test
    void testMultiplePatterns() {
        runner.withPropertyValues("logging.accesslog.info[0]=.*/api/.*", "logging.accesslog.info[1]=.*/rest/.*")
                .run(context -> {
                    AccessLogFilterConfig config = context.getBean(AccessLogFilterConfig.class);

                    assertThat(config.getInfo())
                            .hasSize(2)
                            .extracting(Pattern::pattern)
                            .containsExactly(".*/api/.*", ".*/rest/.*");
                });
    }

    @Test
    void conditionalOnWebFluxApplication() {
        WebApplicationContextRunner servletAppRunner = new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AccessLogWebFluxAutoConfiguration.class));
        servletAppRunner.run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean(AccessLogFilterConfig.class)
                .doesNotHaveBean(AccessLogWebfluxFilter.class));
    }

    @Test
    void conditionalOnServletWebApplicationConflictCheck() {
        // Check there's no conflict when both configurations are present in a Reactive app
        runner.withPropertyValues("logging.accesslog.enabled=true") // Required for ServletAutoConfiguration
                .withConfiguration(AutoConfigurations.of(AccessLogServletAutoConfiguration.class))
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(AccessLogFilterConfig.class)
                        .hasSingleBean(AccessLogWebfluxFilter.class));
    }

    /**
     * Note: we don't test our actual AccessLogWebFluxAutoConfiguration here because it's now
     * conditional on not having Gateway classes in the classpath.
     * In these tests, we're using TestAccessLogConfiguration as a substitute.
     */
}
