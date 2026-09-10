/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gwc.config.core;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

class GwcRequestPathInfoFilterTest {

    /**
     * Requests not dispatched to GWC are left alone: no {@code gwc} segment at all, {@code gwc} as part of a resource
     * name under the REST API ({@code GET /rest/resource/gwc-gs.xml} returned a 500 NPE, see issue #913), a {@code gwc}
     * directory under the REST resource API, a non-existent {@code gwc}-prefixed REST resource, a web UI path with a
     * {@code gwc} segment, a first segment merely starting with {@code gwc}, and a {@code gwc} segment deeper than a
     * virtual service prefix allows.
     */
    @ParameterizedTest
    @ValueSource(
            strings = {
                "/actuator/health",
                "/rest/resource/gwc-gs.xml",
                "/rest/resource/gwc/geowebcache.xml",
                "/rest/resource/gwcfoo",
                "/web/images/gwc/tile.png",
                "/gwcstore/styles.json",
                "/a/b/c/gwc/tile.png"
            })
    void nonGwcRequest_returnsOriginalRequest(String requestURI) {
        MockHttpServletRequest request = mockRequest(requestURI, "");
        HttpServletRequest result = GwcRequestPathInfoFilter.adaptRequest(request);
        assertThat(result).isSameAs(request);
    }

    @Test
    void nonVirtual_emptyContextPath() {
        MockHttpServletRequest request = mockRequest("/gwc/demo/layer:name", "");
        HttpServletRequest result = GwcRequestPathInfoFilter.adaptRequest(request);
        assertThat(result.getServletPath()).isEqualTo("/gwc");
        assertThat(result.getPathInfo()).isEqualTo("/demo/layer:name");
        assertThat(result.getRequestURI()).isEqualTo("/gwc/demo/layer:name");
    }

    @Test
    void nonVirtual_withContextPath() {
        MockHttpServletRequest request = mockRequest("/ctx/gwc/demo/layer:name", "/ctx");
        HttpServletRequest result = GwcRequestPathInfoFilter.adaptRequest(request);
        assertThat(result.getServletPath()).isEqualTo("/gwc");
        assertThat(result.getPathInfo()).isEqualTo("/demo/layer:name");
        assertThat(result.getRequestURI()).isEqualTo("/ctx/gwc/demo/layer:name");
    }

    @Test
    void virtual_emptyContextPath() {
        MockHttpServletRequest request = mockRequest("/ws/gwc/demo/layer:name", "");
        HttpServletRequest result = GwcRequestPathInfoFilter.adaptRequest(request);
        assertThat(result.getServletPath()).isEqualTo("/ws");
        assertThat(result.getPathInfo()).isEqualTo("/demo/layer:name");
        assertThat(result.getRequestURI()).isEqualTo("/gwc/demo/layer:name");
    }

    @Test
    void virtual_withContextPath() {
        MockHttpServletRequest request = mockRequest("/ctx/ws/gwc/demo/layer:name", "/ctx");
        HttpServletRequest result = GwcRequestPathInfoFilter.adaptRequest(request);
        assertThat(result.getServletPath()).isEqualTo("/ws");
        assertThat(result.getPathInfo()).isEqualTo("/demo/layer:name");
        assertThat(result.getRequestURI()).isEqualTo("/ctx/gwc/demo/layer:name");
    }

    @Test
    void virtual_servicePath() {
        MockHttpServletRequest request = mockRequest("/ws/gwc/service/wmts", "");
        HttpServletRequest result = GwcRequestPathInfoFilter.adaptRequest(request);
        assertThat(result.getServletPath()).isEqualTo("/ws");
        assertThat(result.getPathInfo()).isEqualTo("/service/wmts");
        assertThat(result.getRequestURI()).isEqualTo("/gwc/service/wmts");
    }

    @Test
    void virtual_restWebPath() {
        MockHttpServletRequest request = mockRequest("/ws/gwc/rest/web/blobstores", "");
        HttpServletRequest result = GwcRequestPathInfoFilter.adaptRequest(request);
        assertThat(result.getServletPath()).isEqualTo("/ws");
        assertThat(result.getPathInfo()).isEqualTo("/rest/web/blobstores");
        assertThat(result.getRequestURI()).isEqualTo("/gwc/rest/web/blobstores");
    }

    @Test
    void virtual_workspaceAndLayer() {
        MockHttpServletRequest request = mockRequest("/ws/layer/gwc/service/wmts", "");
        HttpServletRequest result = GwcRequestPathInfoFilter.adaptRequest(request);
        assertThat(result.getServletPath()).isEqualTo("/ws/layer");
        assertThat(result.getPathInfo()).isEqualTo("/service/wmts");
        assertThat(result.getRequestURI()).isEqualTo("/gwc/service/wmts");
    }

    @Test
    void virtual_workspaceNameStartingWithGwc() {
        MockHttpServletRequest request = mockRequest("/gwcws/gwc/demo/layer:name", "");
        HttpServletRequest result = GwcRequestPathInfoFilter.adaptRequest(request);
        assertThat(result.getServletPath()).isEqualTo("/gwcws");
        assertThat(result.getPathInfo()).isEqualTo("/demo/layer:name");
        assertThat(result.getRequestURI()).isEqualTo("/gwc/demo/layer:name");
    }

    private MockHttpServletRequest mockRequest(String requestURI, String contextPath) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestURI);
        request.setContextPath(contextPath);
        return request;
    }
}
