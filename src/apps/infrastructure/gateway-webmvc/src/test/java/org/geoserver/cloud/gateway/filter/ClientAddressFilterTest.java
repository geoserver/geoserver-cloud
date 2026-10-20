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
import java.util.Enumeration;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(OutputCaptureExtension.class)
class ClientAddressFilterTest {

    private static final TrustedProxies TRUSTED_PROXIES = TrustedProxies.of(Pattern.compile("10\\.0\\.0\\.\\d+"));

    private final ClientAddressFilter filter = new ClientAddressFilter(TRUSTED_PROXIES);

    @Test
    void reportsClientFromTrustedProxy() throws Exception {
        HttpServletRequest filtered = filter(requestFrom("10.0.0.5", "198.51.100.23"));

        assertThat(filtered.getRemoteAddr()).isEqualTo("198.51.100.23");
        assertThat(filtered.getRemoteHost()).isEqualTo("198.51.100.23");
    }

    @Test
    void ignoresHeaderFromUntrustedClient() throws Exception {
        HttpServletRequest filtered = filter(requestFrom("192.168.1.10", "10.0.0.5"));

        assertThat(filtered.getRemoteAddr()).isEqualTo("192.168.1.10");
        assertThat(filtered.getRemoteHost()).isEqualTo("192.168.1.10");
    }

    @Test
    void keepsConnectionWithoutHeader() throws Exception {
        HttpServletRequest filtered = filter(requestFrom("10.0.0.5"));

        assertThat(filtered.getRemoteAddr()).isEqualTo("10.0.0.5");
    }

    // Spring's ForwardedHeaderFilter runs first: it reports the address of the header, and hides the header
    @Test
    void readsConnectionUnderWrappers() throws Exception {
        HttpServletRequest forwarded = forwardedHeadersApplied(requestFrom("192.168.1.10", "6.6.6.6"));

        HttpServletRequest filtered = filter(forwarded);

        assertThat(filtered.getRemoteAddr()).isEqualTo("192.168.1.10");
    }

    @Test
    void readsHeaderUnderWrappers() throws Exception {
        HttpServletRequest forwarded = forwardedHeadersApplied(requestFrom("10.0.0.5", "198.51.100.23"));

        HttpServletRequest filtered = filter(forwarded);

        assertThat(filtered.getRemoteAddr()).isEqualTo("198.51.100.23");
    }

    @Test
    void warnsOnceAboutUntrustedHeader(CapturedOutput output) throws Exception {
        filter(requestFrom("10.0.0.5", "198.51.100.23"));
        filter(requestFrom("192.168.1.10"));
        assertThat(output).doesNotContain("not a trusted proxy");

        filter(requestFrom("192.168.1.10", "6.6.6.6"));
        filter(requestFrom("192.168.1.11", "6.6.6.7"));
        assertThat(output).containsOnlyOnce("not a trusted proxy").contains("192.168.1.10");
    }

    private static MockHttpServletRequest requestFrom(String connectionAddress, String... forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/wms");
        request.setRemoteAddr(connectionAddress);
        for (String value : forwardedFor) {
            request.addHeader("X-Forwarded-For", value);
        }
        return request;
    }

    /** Wraps the request like Spring's ForwardedHeaderFilter: the header gives the remote address, then it is hidden */
    private static HttpServletRequest forwardedHeadersApplied(MockHttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getRemoteAddr() {
                return forwardedFor;
            }

            @Override
            public String getHeader(String name) {
                return isForwardedFor(name) ? null : super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                return isForwardedFor(name) ? Collections.emptyEnumeration() : super.getHeaders(name);
            }

            private boolean isForwardedFor(String name) {
                return "X-Forwarded-For".equalsIgnoreCase(name);
            }
        };
    }

    /** @return the request passed to the next filter */
    private HttpServletRequest filter(HttpServletRequest request) throws ServletException, IOException {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return (HttpServletRequest) chain.getRequest();
    }
}
