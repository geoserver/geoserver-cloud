/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @since 1.0
 */
class ServiceIdFilterAutoConfigurationTest {

    WebApplicationContextRunner webappRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ServiceIdFilterAutoConfiguration.class))
            .withPropertyValues("info.instance-id=test-instance-id");

    @Test
    void testDisabledByDefault() {
        webappRunner.run(context -> assertThat(context).doesNotHaveBean("serviceIdFilter"));
    }

    @Test
    void testConditionalOnClassServletFilterUnmet() {
        webappRunner
                .withPropertyValues("geoserver.debug.instanceId=true")
                .withClassLoader(new FilteredClassLoader(Filter.class))
                .run(context -> assertThat(context).doesNotHaveBean("serviceIdFilter"));
    }

    @Test
    void testOnWebApplicationConditionUnmet() {
        ApplicationContextRunner notAWebAppRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ServiceIdFilterAutoConfiguration.class))
                .withPropertyValues("geoserver.debug.instanceId=true");

        notAWebAppRunner.run(context -> assertThat(context).doesNotHaveBean("serviceIdFilter"));
    }

    @Test
    void testEnabledAndAllConditionalsMet() {
        webappRunner
                .withPropertyValues( //
                        "geoserver.debug.instanceId=true")
                .run(context -> {
                    assertThat(context).hasBean("serviceIdFilter");
                    Filter servletFilter = context.getBean("serviceIdFilter", Filter.class);
                    ServletRequest req = new MockHttpServletRequest();
                    MockHttpServletResponse resp = new MockHttpServletResponse();
                    FilterChain chain = mock(FilterChain.class);
                    servletFilter.doFilter(req, resp, chain);
                    assertEquals("test-instance-id", resp.getHeader("X-gs-cloud-service-id"));
                    verify(chain).doFilter(req, resp);
                });
    }
}
