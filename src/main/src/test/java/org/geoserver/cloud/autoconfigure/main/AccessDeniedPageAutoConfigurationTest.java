/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.main;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

/** Tests for {@link AccessDeniedPageAutoConfiguration}. */
class AccessDeniedPageAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AccessDeniedPageAutoConfiguration.class));

    /** Upstream's {@code filePublisherMapping} is registered at {@code -50}; this one must be asked first. */
    @Test
    void mappingTakesPrecedenceOverTheFilePublisherMapping() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(SimpleUrlHandlerMapping.class);
            SimpleUrlHandlerMapping mapping = context.getBean(SimpleUrlHandlerMapping.class);
            assertThat(mapping.getOrder()).isLessThan(-50);
            assertThat(mapping.getUrlMap()).containsOnlyKeys("/accessDenied.html");
        });
    }

    /**
     * The page comes from the classpath, jar or not, and the 403 status set by the exception translation filter before
     * forwarding must survive the page being written.
     */
    @Test
    void servesThePageFromTheClasspathKeepingTheForbiddenStatus() {
        runner.run(context -> {
            MockHttpServletResponse response = forwardDeniedRequest(context, "GET");

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
            assertThat(response.getContentType()).startsWith("text/html");
            assertThat(response.getContentAsString()).contains("Access Denied");
        });
    }

    // The forwarded request keeps the method of the denied one
    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void deniedWritesStayForbidden(String method) {
        runner.run(context -> {
            MockHttpServletResponse response = forwardDeniedRequest(context, method);

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
            assertThat(response.getContentAsString()).contains("Access Denied");
        });
    }

    /** Handles the request forwarded by an exception translation filter after it set the 403 status. */
    private MockHttpServletResponse forwardDeniedRequest(ApplicationContext context, String method) throws Exception {
        SimpleUrlHandlerMapping mapping = context.getBean(SimpleUrlHandlerMapping.class);
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/accessDenied.html");
        ServletRequestPathUtils.parseAndCache(request);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        HandlerExecutionChain chain = mapping.getHandler(request);
        assertThat(chain).isNotNull();
        HttpRequestHandler handler = (HttpRequestHandler) chain.getHandler();
        for (HandlerInterceptor interceptor : chain.getInterceptorList()) {
            interceptor.preHandle(request, response, handler);
        }
        handler.handleRequest(request, response);
        return response;
    }
}
