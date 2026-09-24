/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gateway.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.cloud.gateway.server.mvc.handler.RestClientProxyExchange;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

/**
 * Proxies backend responses with a {@code Content-Type} rejected by Spring.
 *
 * <p>Spring Cloud Gateway parses the {@code Content-Type} of every proxied response to decide whether to flush after
 * each chunk, as required by streaming media types like {@code text/event-stream}. Spring rejects the GML 2 and GML 3.1
 * media types used by GeoServer, such as {@code text/xml; subtype=gml/3.1.1} or {@code application/vnd.ogc.gml/3.1.1},
 * because they are not valid under RFC 9110. The result was an HTTP 500 for every WFS 1.x GML response (issue #1002).
 * The OGC specifications require those exact strings, hence the gateway has to tolerate them.
 *
 * <p>Responses with an unparsable {@code Content-Type} are copied through without per-chunk flushing and with the
 * header forwarded verbatim; all other responses take the default Spring Cloud Gateway path. Upstream issue: <a
 * href="https://github.com/spring-cloud/spring-cloud-gateway/issues/3695">spring-cloud-gateway#3695</a>.
 *
 * @since 3.1.0
 */
@Slf4j
public class LenientContentTypeProxyExchange extends RestClientProxyExchange {

    public LenientContentTypeProxyExchange(RestClient restClient, GatewayMvcProperties properties) {
        super(restClient, properties);
    }

    @Override
    protected int copyResponseBody(
            ClientHttpResponse clientResponse, InputStream inputStream, OutputStream outputStream) throws IOException {
        if (hasParsableContentType(clientResponse)) {
            return super.copyResponseBody(clientResponse, inputStream, outputStream);
        }
        log.debug(
                "Unparsable Content-Type on proxied response, relaying it without per-chunk flushing: {}",
                clientResponse.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
        return StreamUtils.copy(inputStream, outputStream);
    }

    @Override
    protected void closeResponse(
            ClientHttpResponse clientResponse, InputStream inputStream, @Nullable IOException copyException) {
        if (hasParsableContentType(clientResponse)) {
            super.closeResponse(clientResponse, inputStream, copyException);
        } else {
            clientResponse.close();
        }
    }

    private static boolean hasParsableContentType(ClientHttpResponse clientResponse) {
        try {
            clientResponse.getHeaders().getContentType();
            return true;
        } catch (InvalidMediaTypeException unparsable) {
            return false;
        }
    }
}
