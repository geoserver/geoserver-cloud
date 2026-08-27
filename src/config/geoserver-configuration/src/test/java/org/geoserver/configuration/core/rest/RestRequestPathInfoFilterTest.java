/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.core.rest;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Tests {@link RestRequestPathInfoFilter#adaptRequest}: every request under the {@code /rest} base path must be
 * adapted, including paths that merely contain {@code /gwc} in a resource name (issue #913), while GWC-dispatched URLs
 * such as {@code /gwc/rest/**} are left alone.
 */
class RestRequestPathInfoFilterTest {

    @Test
    void plainRestPath_adapted() {
        HttpServletRequest result = adapt("/rest/workspaces.json", "");
        assertThat(result.getServletPath()).isEqualTo("/rest");
        assertThat(result.getPathInfo()).isEqualTo("/workspaces.json");
        assertThat(result.getRequestURI()).isEqualTo("/rest/workspaces.json");
    }

    /** {@code GET /rest/resource/gwc-gs.xml} returned a 500 NPE, see issue #913 */
    @Test
    void restResourceFileNameContainingGwc_adapted() {
        HttpServletRequest result = adapt("/rest/resource/gwc-gs.xml", "");
        assertThat(result.getServletPath()).isEqualTo("/rest");
        assertThat(result.getPathInfo()).isEqualTo("/resource/gwc-gs.xml");
        assertThat(result.getRequestURI()).isEqualTo("/rest/resource/gwc-gs.xml");
    }

    @Test
    void restResourceGwcDirectory_adapted() {
        HttpServletRequest result = adapt("/rest/resource/gwc/geowebcache.xml", "");
        assertThat(result.getServletPath()).isEqualTo("/rest");
        assertThat(result.getPathInfo()).isEqualTo("/resource/gwc/geowebcache.xml");
    }

    @Test
    void restResourceNonExistentGwcPrefixedPath_adapted() {
        HttpServletRequest result = adapt("/rest/resource/gwcfoo", "");
        assertThat(result.getServletPath()).isEqualTo("/rest");
        assertThat(result.getPathInfo()).isEqualTo("/resource/gwcfoo");
    }

    @Test
    void restWithContextPath_adapted() {
        HttpServletRequest result = adapt("/ctx/rest/resource/gwc-gs.xml", "/ctx");
        assertThat(result.getServletPath()).isEqualTo("/rest");
        assertThat(result.getPathInfo()).isEqualTo("/resource/gwc-gs.xml");
        assertThat(result.getRequestURI()).isEqualTo("/ctx/rest/resource/gwc-gs.xml");
    }

    @Test
    void restBasePathAlone_adapted() {
        HttpServletRequest result = adapt("/rest", "");
        assertThat(result.getServletPath()).isEqualTo("/rest");
        assertThat(result.getPathInfo()).isEmpty();
    }

    @Test
    void gwcRestApi_notAdapted() {
        MockHttpServletRequest request = mockRequest("/gwc/rest/layers.json", "");
        assertThat(RestRequestPathInfoFilter.adaptRequest(request)).isSameAs(request);
    }

    @Test
    void virtualServiceGwcRestApi_notAdapted() {
        MockHttpServletRequest request = mockRequest("/ws/gwc/rest/layers.json", "");
        assertThat(RestRequestPathInfoFilter.adaptRequest(request)).isSameAs(request);
    }

    @Test
    void nonRestPath_notAdapted() {
        MockHttpServletRequest request = mockRequest("/actuator/health", "");
        assertThat(RestRequestPathInfoFilter.adaptRequest(request)).isSameAs(request);
    }

    @Test
    void restNotAtSegmentBoundary_notAdapted() {
        MockHttpServletRequest request = mockRequest("/restful/thing", "");
        assertThat(RestRequestPathInfoFilter.adaptRequest(request)).isSameAs(request);
    }

    private HttpServletRequest adapt(String requestURI, String contextPath) {
        return RestRequestPathInfoFilter.adaptRequest(mockRequest(requestURI, contextPath));
    }

    private MockHttpServletRequest mockRequest(String requestURI, String contextPath) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestURI);
        request.setContextPath(contextPath);
        return request;
    }
}
