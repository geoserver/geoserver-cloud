/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gwc.config.core;

import static org.assertj.core.api.Assertions.assertThat;

import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class GwcRequestPathInfoFilterTest {

    @Test
    void nonGwcUrl_returnsOriginalRequest() {
        MockHttpServletRequest request = mockRequest("/actuator/health", "");
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
        assertThat(result.getRequestURI()).isEqualTo("/ws/gwc/service/wmts");
    }

    @Test
    void virtual_servicePath_withContextPath() {
        MockHttpServletRequest request = mockRequest("/ctx/ws/gwc/service/wmts", "/ctx");
        HttpServletRequest result = GwcRequestPathInfoFilter.adaptRequest(request);
        assertThat(result.getServletPath()).isEqualTo("/ws");
        assertThat(result.getPathInfo()).isEqualTo("/service/wmts");
        assertThat(result.getRequestURI()).isEqualTo("/ctx/ws/gwc/service/wmts");
    }

    @Test
    void virtual_servicePath_withWorkspaceAndLayer() {
        MockHttpServletRequest request = mockRequest("/ctx/ws/layer/gwc/service/wmts", "/ctx");
        HttpServletRequest result = GwcRequestPathInfoFilter.adaptRequest(request);
        assertThat(result.getServletPath()).isEqualTo("/ws/layer");
        assertThat(result.getPathInfo()).isEqualTo("/service/wmts");
        assertThat(result.getRequestURI()).isEqualTo("/ctx/ws/layer/gwc/service/wmts");
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
    void virtual_demoPath_stillStripsWorkspace() {
        MockHttpServletRequest request = mockRequest("/ctx/ws/gwc/demo/layer:name", "/ctx");
        HttpServletRequest result = GwcRequestPathInfoFilter.adaptRequest(request);
        assertThat(result.getServletPath()).isEqualTo("/ws");
        assertThat(result.getPathInfo()).isEqualTo("/demo/layer:name");
        assertThat(result.getRequestURI()).isEqualTo("/ctx/gwc/demo/layer:name");
    }

    private MockHttpServletRequest mockRequest(String requestURI, String contextPath) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestURI);
        request.setContextPath(contextPath);
        return request;
    }
}
