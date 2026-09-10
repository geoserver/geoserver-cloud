/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/** @since 3.0.0 */
class StripBasePathTest {

    @Test
    void prefixNotStartingWithSlash_throws() {
        assertThatIllegalStateException().isThrownBy(() -> new StripBasePath("foo"));
    }

    @Test
    void prefixEndingWithSlash_throws() {
        assertThatIllegalStateException().isThrownBy(() -> new StripBasePath("/foo/"));
    }

    @Test
    void rootSlashPrefix_doesNotThrow() {
        assertThatNoException().isThrownBy(() -> new StripBasePath("/"));
    }

    @Test
    void nullPrefix_passesThrough() throws Exception {
        assertThat(filterPath(null, "/a/b")).isEqualTo("/a/b");
    }

    @Test
    void rootPrefix_passesThrough() throws Exception {
        assertThat(filterPath("/", "/wms")).isEqualTo("/wms");
    }

    @Test
    void zeroArgShortcut_passesThrough() throws Exception {
        HandlerFilterFunction<ServerResponse, ServerResponse> filter = GeoServerGatewayFilterFunctions.stripBasePath();
        assertThat(applyFilter(filter, "/wms")).isEqualTo("/wms");
    }

    @Test
    void rootPrefix_rootRequest_passesThrough() throws Exception {
        assertThat(filterPath("/", "/")).isEqualTo("/");
    }

    @Test
    void singleSegment_strips() throws Exception {
        assertThat(filterPath("/geoserver", "/geoserver/ows")).isEqualTo("/ows");
    }

    @Test
    void multiSegment_strips() throws Exception {
        assertThat(filterPath("/geoserver/cloud", "/geoserver/cloud/ows")).isEqualTo("/ows");
    }

    @Test
    void prefixNotMatching_passesThrough() throws Exception {
        assertThat(filterPath("/geoserver", "/other/ows")).isEqualTo("/other/ows");
    }

    @Test
    void rootPathEqualsPrefix_passesThrough() throws Exception {
        assertThat(filterPath("/geoserver", "/geoserver")).isEqualTo("/geoserver");
    }

    @Test
    void deepPath_stripsCorrectly() throws Exception {
        assertThat(filterPath("/a/b/c", "/a/b/c/d/e")).isEqualTo("/d/e");
    }

    @Test
    void queryWithBareEqualsSign_isKeptVerbatim() throws Exception {
        ServerRequest stripped = filterRequest("/geoserver/cloud", "/geoserver/cloud/ows", "CQL_FILTER=1=1");

        assertThat(stripped.uri().getRawPath()).isEqualTo("/ows");
        assertThat(stripped.uri().getRawQuery()).isEqualTo("CQL_FILTER=1=1");
    }

    @Test
    void percentEncodedQuery_isKeptVerbatim() throws Exception {
        ServerRequest stripped = filterRequest("/geoserver/cloud", "/geoserver/cloud/ows", "layers=a%3Db%20c");

        assertThat(stripped.uri().getRawQuery()).isEqualTo("layers=a%3Db%20c");
    }

    /** The X-Forwarded-Prefix header is derived from the original URL recorded by the filter. */
    @Test
    void originalRequestUrl_isRecorded() throws Exception {
        ServerRequest stripped = filterRequest("/geoserver/cloud", "/geoserver/cloud/ows", "service=WMS");

        LinkedHashSet<URI> originalUrls = MvcUtils.getAttribute(stripped, MvcUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
        assertThat(originalUrls).containsExactly(URI.create("http://localhost/geoserver/cloud/ows?service=WMS"));
    }

    private ServerRequest filterRequest(String prefix, String requestPath, String queryString) throws Exception {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", requestPath);
        mockRequest.setQueryString(queryString);
        ServerRequest request = ServerRequest.create(mockRequest, List.of());
        AtomicReference<ServerRequest> captured = new AtomicReference<>();
        GeoServerGatewayFilterFunctions.stripBasePath(prefix).filter(request, req -> {
            captured.set(req);
            return ServerResponse.ok().build();
        });
        return captured.get();
    }

    private String filterPath(String prefix, String requestPath) throws Exception {
        return applyFilter(GeoServerGatewayFilterFunctions.stripBasePath(prefix), requestPath);
    }

    private String applyFilter(HandlerFilterFunction<ServerResponse, ServerResponse> filter, String requestPath)
            throws Exception {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", requestPath);
        ServerRequest request = ServerRequest.create(mockRequest, List.of());
        AtomicReference<String> captured = new AtomicReference<>();
        filter.filter(request, req -> {
            captured.set(req.uri().getRawPath());
            return ServerResponse.ok().build();
        });
        return captured.get();
    }
}
