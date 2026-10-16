/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.function.ServerRequest;

class RequestIdHeadersFilterTest {

    private final RequestIdHeadersFilter filter = new RequestIdHeadersFilter();

    private final ServerRequest request = ServerRequest.create(new MockHttpServletRequest(), List.of());

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void addsRequestId() {
        MDC.put("http.request.id", "01k2gateway");

        HttpHeaders headers = filter.apply(new HttpHeaders(), request);

        assertThat(headers.get("X-Request-ID")).containsExactly("01k2gateway");
    }

    // The gateway took its request id from one of these headers, or created a new one
    @Test
    void replacesIncomingRequestIds() {
        MDC.put("http.request.id", "01k2gateway");
        HttpHeaders input = new HttpHeaders();
        input.set("X-Request-ID", "proxy-request-id");
        input.set("trace-id", "proxy-trace-id");
        input.set("http.request.id", "proxy-http-request-id");
        input.set(HttpHeaders.ACCEPT, "text/xml");

        HttpHeaders headers = filter.apply(input, request);

        assertThat(headers.get("X-Request-ID")).containsExactly("01k2gateway");
        assertThat(headers.containsHeader("trace-id")).isFalse();
        assertThat(headers.containsHeader("http.request.id")).isFalse();
        assertThat(headers.getFirst(HttpHeaders.ACCEPT)).isEqualTo("text/xml");
    }

    // There is no request id in the MDC if logging.mdc.include.http.id is false
    @Test
    void withoutRequestId() {
        HttpHeaders input = new HttpHeaders();
        input.set("X-Request-ID", "proxy-request-id");
        input.set(HttpHeaders.ACCEPT, "text/xml");

        HttpHeaders headers = filter.apply(input, request);

        assertThat(headers.containsHeader("X-Request-ID")).isFalse();
        assertThat(headers.getFirst(HttpHeaders.ACCEPT)).isEqualTo("text/xml");
    }
}
