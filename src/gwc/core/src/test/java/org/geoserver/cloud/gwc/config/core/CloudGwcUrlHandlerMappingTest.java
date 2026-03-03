/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gwc.config.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.RequestPath;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.ServletRequestPathUtils;

class CloudGwcUrlHandlerMappingTest {

    private Catalog catalog;
    private CloudGwcUrlHandlerMapping handler;

    @BeforeEach
    void setUp() {
        catalog = mock(Catalog.class);
        handler = new CloudGwcUrlHandlerMapping(catalog, "/gwc/demo");
    }

    @Test
    void patternNotFound_returnsNull() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/other/path");
        RequestPath originalPath = RequestPath.parse("/other/path", "");
        request.setAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE, originalPath);

        HandlerMethod result = handler.lookupHandlerMethod("/other/path", request);

        assertThat(result).isNull();
        assertThat(request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE)).isSameAs(originalPath);
    }

    @Test
    void validWorkspace_pathAttributeUpdatedDuringSuperCall() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myws/gwc/demo/layer:name");
        request.setServletPath("/myws");
        RequestPath originalPath = RequestPath.parse("/myws/gwc/demo/layer:name", "");
        request.setAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE, originalPath);

        AtomicReference<Object> pathDuringCatalogCall = new AtomicReference<>();
        when(catalog.getWorkspaceByName(eq("myws"))).thenAnswer(invocation -> {
            pathDuringCatalogCall.set(request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE));
            return mock(WorkspaceInfo.class);
        });

        HandlerMethod result = handler.lookupHandlerMethod("/myws/gwc/demo/layer:name", request);

        // No handlers registered, so result is null — we're testing attribute management
        assertThat(result).isNull();

        // During the super call, PATH_ATTRIBUTE was set to the stripped path
        Object capturedPath = pathDuringCatalogCall.get();
        assertThat(capturedPath).isNotNull().isInstanceOf(RequestPath.class);
        assertThat(((RequestPath) capturedPath).value()).isEqualTo("/gwc/demo/layer:name");

        // After return, PATH_ATTRIBUTE is restored to the original
        assertThat(request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE)).isSameAs(originalPath);
    }

    @Test
    void pathAttributeRestoredOnException() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myws/gwc/demo/layer:name");
        request.setServletPath("/myws");
        RequestPath originalPath = RequestPath.parse("/myws/gwc/demo/layer:name", "");
        request.setAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE, originalPath);

        RuntimeException catalogException = new RuntimeException("catalog error");
        when(catalog.getWorkspaceByName(eq("myws"))).thenThrow(catalogException);

        assertThatThrownBy(() -> handler.lookupHandlerMethod("/myws/gwc/demo/layer:name", request))
                .isSameAs(catalogException);

        // PATH_ATTRIBUTE must be restored even after exception
        assertThat(request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE)).isSameAs(originalPath);
    }

    @Test
    void nullOriginalPathAttribute_removedAfterCall() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myws/gwc/demo/layer:name");
        request.setServletPath("/myws");
        // Do NOT set PATH_ATTRIBUTE — simulates it being absent

        when(catalog.getWorkspaceByName(eq("myws"))).thenReturn(mock(WorkspaceInfo.class));

        handler.lookupHandlerMethod("/myws/gwc/demo/layer:name", request);

        // After call, attribute should be removed (not set to null)
        assertThat(request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE)).isNull();
    }
}
