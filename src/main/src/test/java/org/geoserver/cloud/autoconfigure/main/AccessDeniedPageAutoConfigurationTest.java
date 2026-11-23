/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.main;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Tests for {@link AccessDeniedPageAutoConfiguration}. */
class AccessDeniedPageAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AccessDeniedPageAutoConfiguration.class));

    // GeoServer's exception translation filters forward a denied request to this path
    @Test
    void servletAnswersThePagePath() {
        runner.run(context -> {
            ServletRegistrationBean<?> registration = context.getBean(ServletRegistrationBean.class);
            assertThat(registration.getUrlMappings()).containsExactly("/accessDenied.html");
        });
    }

    /**
     * The page comes from the classpath, jar or not, and the 403 status set by the exception translation filter before
     * forwarding must survive the page being written.
     */
    @Test
    void writesThePageKeepingTheForbiddenStatus() {
        runner.run(context -> {
            MockHttpServletResponse response = forwardDeniedRequest(context, "GET", "/accessDenied.html");

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
            MockHttpServletResponse response = forwardDeniedRequest(context, method, "/accessDenied.html");

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
            assertThat(response.getContentAsString()).contains("Access Denied");
        });
    }

    @Test
    void writesThePageWhateverThePath() {
        runner.run(context -> {
            MockHttpServletResponse response = forwardDeniedRequest(context, "GET", "/rest/workspaces");

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
            assertThat(response.getContentAsString()).contains("Access Denied");
        });
    }

    // The forward keeps the URL of the denied request, against which the browser resolves the link of the page
    @ParameterizedTest
    @CsvSource({
        "/geoserver/rest/security/usergroup/users.json, ../../../",
        "/geoserver/gwc/rest/layers/, ../../../",
        "/geoserver/rest/workspaces.json, ../"
    })
    void baseLeadsLinkToServiceRoot(String deniedUri, String base) {
        runner.run(context -> {
            MockHttpServletResponse response = handle(context, forwardedFrom("/geoserver", deniedUri));

            assertThat(response.getContentAsString()).contains("<base href=\"%s\" />".formatted(base));
        });
    }

    @Test
    void noBaseAtTheServiceRoot() {
        runner.run(context -> {
            MockHttpServletResponse forwarded = handle(context, forwardedFrom("/geoserver", "/geoserver/rest"));
            MockHttpServletResponse direct = forwardDeniedRequest(context, "GET", "/geoserver/accessDenied.html");

            assertThat(forwarded.getContentAsString()).contains("Access Denied").doesNotContain("<base");
            assertThat(direct.getContentAsString()).contains("Access Denied").doesNotContain("<base");
        });
    }

    /** Handles the request forwarded by an exception translation filter after it set the 403 status. */
    private static MockHttpServletResponse forwardDeniedRequest(ApplicationContext context, String method, String path)
            throws Exception {
        return handle(context, new MockHttpServletRequest(method, path));
    }

    /**
     * The request forwarded to the page, with the attributes kept by the servlet container about the denied request.
     */
    private static MockHttpServletRequest forwardedFrom(String contextPath, String deniedUri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", contextPath + "/accessDenied.html");
        request.setContextPath(contextPath);
        request.setAttribute(RequestDispatcher.FORWARD_CONTEXT_PATH, contextPath);
        request.setAttribute(RequestDispatcher.FORWARD_REQUEST_URI, deniedUri);
        return request;
    }

    private static MockHttpServletResponse handle(ApplicationContext context, MockHttpServletRequest request)
            throws Exception {
        ServletRegistrationBean<?> registration = context.getBean(ServletRegistrationBean.class);
        Servlet servlet = registration.getServlet();
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        servlet.service(request, response);
        return response;
    }
}
