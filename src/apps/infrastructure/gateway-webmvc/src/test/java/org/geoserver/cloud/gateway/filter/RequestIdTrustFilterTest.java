/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdTrustFilterTest {

    private static final TrustedProxies TRUSTED_PROXIES = TrustedProxies.of(Pattern.compile("10\\.0\\.0\\.\\d+"));

    private final RequestIdTrustFilter filter = new RequestIdTrustFilter(TRUSTED_PROXIES);

    @Test
    void hidesFromUntrustedClient() throws Exception {
        HttpServletRequest filtered = filter(filter, requestFrom("192.168.1.10"));

        assertThat(filtered.getHeader("X-Request-ID")).isNull();
        assertThat(Collections.list(filtered.getHeaders("trace-id"))).isEmpty();
        List<String> headerNames = Collections.list(filtered.getHeaderNames());
        assertThat(headerNames).containsExactly("Accept");
    }

    @Test
    void keepsFromTrustedProxy() throws Exception {
        HttpServletRequest filtered = filter(filter, requestFrom("10.0.0.5"));

        assertThat(filtered.getHeader("X-Request-ID")).isEqualTo("proxy-request-id");
        assertThat(filtered.getHeader("trace-id")).isEqualTo("proxy-trace-id");
        assertThat(filtered.getHeader("http.request.id")).isEqualTo("proxy-http-request-id");
    }

    @Test
    void trustsNoProxyByDefault() throws Exception {
        RequestIdTrustFilter defaultFilter = new RequestIdTrustFilter(TrustedProxies.none());

        HttpServletRequest filtered = filter(defaultFilter, requestFrom("10.0.0.5"));

        assertThat(filtered.getHeader("X-Request-ID")).isNull();
    }

    // The remote address of a request may come from its X-Forwarded-For header, set by the client
    @Test
    void checksConnectionAddress() throws Exception {
        HttpServletRequest forwarded = new HttpServletRequestWrapper(requestFrom("192.168.1.10")) {
            @Override
            public String getRemoteAddr() {
                return "10.0.0.5";
            }
        };

        HttpServletRequest filtered = filter(filter, forwarded);

        assertThat(filtered.getHeader("X-Request-ID")).isNull();
    }

    private static MockHttpServletRequest requestFrom(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/wms");
        request.setRemoteAddr(remoteAddress);
        request.addHeader("Accept", "text/xml");
        request.addHeader("x-request-id", "proxy-request-id");
        request.addHeader("trace-id", "proxy-trace-id");
        request.addHeader("http.request.id", "proxy-http-request-id");
        return request;
    }

    /** @return the request passed to the next filter */
    private static HttpServletRequest filter(RequestIdTrustFilter filter, HttpServletRequest request)
            throws ServletException, IOException {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return (HttpServletRequest) chain.getRequest();
    }
}
