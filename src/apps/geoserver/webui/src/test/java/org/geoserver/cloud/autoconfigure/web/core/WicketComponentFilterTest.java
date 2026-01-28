/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.web.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Test for {@link WicketComponentFilter}
 */
class WicketComponentFilterTest {

    private WicketComponentFilter filter;
    private WebUIConfigurationProperties config;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws IOException {
        config = new WebUIConfigurationProperties();
        filter = new WicketComponentFilter(config);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);

        responseWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(responseWriter);
        when(response.getWriter()).thenReturn(writer);
    }

    @Test
    void testNonBookmarkableUrl() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/web/some/other/url");

        filter.doFilter(request, response, chain);

        // Should pass through to the filter chain
        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void testEnabledComponent() throws ServletException, IOException {
        // WMS is enabled by default
        assertTrue(config.getWms().isEnabled());

        when(request.getRequestURI()).thenReturn("/web/wicket/bookmarkable/org.geoserver.wms.web.WMSAdminPage");

        filter.doFilter(request, response, chain);

        // Should pass through to the filter chain
        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void testEnabledComponentAlternativeUrlFormat() throws ServletException, IOException {
        // WMS is enabled by default
        assertTrue(config.getWms().isEnabled());

        when(request.getRequestURI())
                .thenReturn("/geoserver/cloud/web/bookmarkable/org.geoserver.wms.web.WMSAdminPage");

        filter.doFilter(request, response, chain);

        // Should pass through to the filter chain
        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void testDisabledComponent() throws ServletException, IOException {
        // Disable WMS
        config.getWms().setEnabled(false);
        assertFalse(config.getWms().isEnabled());

        when(request.getRequestURI()).thenReturn("/web/wicket/bookmarkable/org.geoserver.wms.web.WMSAdminPage");

        filter.doFilter(request, response, chain);

        // Should return 404 and not continue the chain
        verify(response).setStatus(HttpStatus.NOT_FOUND.value());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void testDisabledNestedComponent() throws ServletException, IOException {
        // Disable demo page layer preview
        config.getDemos().getLayerPreviewPage().setEnabled(false);

        when(request.getRequestURI()).thenReturn("/web/wicket/bookmarkable/org.geoserver.web.demo.LayerPreviewPage");

        filter.doFilter(request, response, chain);

        // Should return 404 and not continue the chain
        verify(response).setStatus(HttpStatus.NOT_FOUND.value());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void testDisabledNestedComponentAlternativeUrlFormat() throws ServletException, IOException {
        // Disable resource browser tool
        config.getTools().setResourceBrowser(false);

        when(request.getRequestURI())
                .thenReturn("/geoserver/cloud/web/bookmarkable/org.geoserver.web.resources.PageResourceBrowser");

        filter.doFilter(request, response, chain);

        // Should return 404 and not continue the chain
        verify(response).setStatus(HttpStatus.NOT_FOUND.value());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void testUrlsWithQueryParameters() throws ServletException, IOException {
        // Disable WMS
        config.getWms().setEnabled(false);

        when(request.getRequestURI())
                .thenReturn("/web/wicket/bookmarkable/org.geoserver.wms.web.WMSAdminPage?param=value");

        filter.doFilter(request, response, chain);

        // Should return 404 and not continue the chain
        verify(response).setStatus(HttpStatus.NOT_FOUND.value());
        verify(chain, never()).doFilter(request, response);
    }
}
