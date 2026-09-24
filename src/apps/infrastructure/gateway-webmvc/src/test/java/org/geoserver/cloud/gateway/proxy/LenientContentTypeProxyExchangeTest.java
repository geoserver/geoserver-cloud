/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gateway.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

/**
 * Unit tests for the {@link LenientContentTypeProxyExchange} overrides with a stub backend response, covering the
 * copy-failure path of {@code closeResponse} that a live gateway does not reach.
 */
class LenientContentTypeProxyExchangeTest {

    private static final String UNPARSABLE_CONTENT_TYPE = "text/xml; subtype=gml/3.1.1";

    private static final byte[] BODY = "<xsd:schema/>".getBytes(StandardCharsets.UTF_8);

    private LenientContentTypeProxyExchange proxyExchange;

    @BeforeEach
    void setUp() {
        proxyExchange = new LenientContentTypeProxyExchange(RestClient.create(), new GatewayMvcProperties());
    }

    @Test
    void copiesBodyWhenContentTypeCannotBeParsed() throws IOException {
        StubClientHttpResponse response = new StubClientHttpResponse(UNPARSABLE_CONTENT_TYPE);
        ByteArrayOutputStream copied = new ByteArrayOutputStream();

        int transferred = proxyExchange.copyResponseBody(response, response.getBody(), copied);

        assertThat(transferred).isEqualTo(BODY.length);
        assertThat(copied.toByteArray()).isEqualTo(BODY);
    }

    @Test
    void closesResponseAfterCopyFailureWhenContentTypeCannotBeParsed() {
        StubClientHttpResponse response = new StubClientHttpResponse(UNPARSABLE_CONTENT_TYPE);
        IOException copyFailure = new IOException("client went away");

        assertThatCode(() -> proxyExchange.closeResponse(response, response.getBody(), copyFailure))
                .doesNotThrowAnyException();

        assertThat(response.closed).isTrue();
    }

    @Test
    void closesResponseAfterCopyFailureWhenContentTypeIsWellFormed() {
        StubClientHttpResponse response = new StubClientHttpResponse("application/gml+xml; version=3.2");
        IOException copyFailure = new IOException("client went away");

        proxyExchange.closeResponse(response, response.getBody(), copyFailure);

        assertThat(response.closed).isTrue();
    }

    /** Backend response with a raw {@code Content-Type} header, recording whether it was closed */
    private static final class StubClientHttpResponse implements ClientHttpResponse {

        private final HttpHeaders headers = new HttpHeaders();

        private final InputStream body = new ByteArrayInputStream(BODY);

        private boolean closed;

        StubClientHttpResponse(String contentType) {
            headers.set(HttpHeaders.CONTENT_TYPE, contentType);
        }

        @Override
        public HttpStatusCode getStatusCode() {
            return HttpStatus.OK;
        }

        @Override
        public String getStatusText() {
            return HttpStatus.OK.getReasonPhrase();
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public InputStream getBody() {
            return body;
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }
}
