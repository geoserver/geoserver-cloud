/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.geotools;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import org.geotools.http.HTTPResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies the response body reaches callers decoded, the way GeoTools' {@code MultithreadedHttpClient} this class is a
 * copy of delivers it, whether or not the origin compresses it.
 */
class SpringEnvironmentAwareGeoToolsHttpClientTest {

    private static final String PAYLOAD = "<WMS_Capabilities version=\"1.3.0\"/>";

    private HttpServer server;

    private SpringEnvironmentAwareGeoToolsHttpClient client;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/plain", exchange -> respond(exchange, PAYLOAD.getBytes(StandardCharsets.UTF_8), false));
        server.createContext("/gzipped", exchange -> respond(exchange, gzip(PAYLOAD), true));
        server.start();
        client = new SpringEnvironmentAwareGeoToolsHttpClient(new GeoToolsHttpClientProxyConfigurationProperties());
    }

    @AfterEach
    void stopServer() {
        client.close();
        server.stop(0);
    }

    @Test
    void plainResponseIsReadableAsIs() throws IOException {
        assertThat(body("/plain")).isEqualTo(PAYLOAD);
    }

    /**
     * Apache HttpClient decompresses the entity on its own, and since 5.6 leaves {@code Content-Encoding} on the
     * response, which used to be the signal to decompress it a second time.
     */
    @Test
    void gzippedResponseIsDecodedOnce() throws IOException {
        assertThat(body("/gzipped")).isEqualTo(PAYLOAD);
    }

    private String body(String path) throws IOException {
        URL url = URI.create("http://%s:%d%s"
                        .formatted(
                                server.getAddress().getHostString(),
                                server.getAddress().getPort(),
                                path))
                .toURL();
        HTTPResponse response = client.get(url);
        try (InputStream in = response.getResponseStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            response.dispose();
        }
    }

    private static void respond(HttpExchange exchange, byte[] body, boolean gzipped) throws IOException {
        if (gzipped) {
            exchange.getResponseHeaders().add("Content-Encoding", "gzip");
        }
        exchange.getResponseHeaders().add("Content-Type", "text/xml");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static byte[] gzip(String content) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return bytes.toByteArray();
    }
}
