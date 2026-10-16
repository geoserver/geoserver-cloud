/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gateway.filter;

import static org.geoserver.cloud.logging.mdc.config.HttpRequestMdcConfigProperties.REQUEST_ID_HEADERS;

import org.geoserver.cloud.logging.mdc.config.HttpRequestMdcConfigProperties;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.server.mvc.filter.HttpHeadersFilter.RequestHttpHeadersFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.function.ServerRequest;

/**
 * Sends the request id of the gateway to the services, in the {@code X-Request-ID} header of the proxied requests.
 *
 * <p>The gateway and the services log each request with an id. A service takes the id from the request headers, and
 * creates a new one if there is none. Without this header, the gateway and the service would log the same request with
 * different ids. The filter removes the other request id headers: the services only read the id logged by the gateway.
 *
 * <p>If there is no request id in the MDC, as when {@code logging.mdc.include.http.id} is false, the services create
 * their own.
 *
 * @see RequestIdTrustFilter
 * @see HttpRequestMdcConfigProperties#findOrCreateRequestId
 */
public class RequestIdHeadersFilter implements RequestHttpHeadersFilter {

    static final String X_REQUEST_ID = "X-Request-ID";

    /** The MDC property holding the request id, named after the request header read by the services */
    private static final String REQUEST_ID_MDC_PROPERTY = HttpRequestMdcConfigProperties.REQUEST_ID_HEADER;

    @Override
    public HttpHeaders apply(HttpHeaders input, ServerRequest request) {
        HttpHeaders headers = HttpHeaders.copyOf(input);
        REQUEST_ID_HEADERS.forEach(headers::remove);
        String requestId = MDC.get(REQUEST_ID_MDC_PROPERTY);
        if (requestId != null) {
            headers.set(X_REQUEST_ID, requestId);
        }
        return headers;
    }
}
