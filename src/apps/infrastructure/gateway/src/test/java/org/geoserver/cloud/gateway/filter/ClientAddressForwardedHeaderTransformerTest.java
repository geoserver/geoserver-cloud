/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

@ExtendWith(OutputCaptureExtension.class)
class ClientAddressForwardedHeaderTransformerTest {

    private static final TrustedProxies TRUSTED_PROXIES = TrustedProxies.of(Pattern.compile("10\\.0\\.0\\.\\d+"));

    private final ClientAddressForwardedHeaderTransformer transformer =
            new ClientAddressForwardedHeaderTransformer(TRUSTED_PROXIES);

    @Test
    void reportsClientFromTrustedProxy() {
        ServerHttpRequest applied = transformer.apply(requestFrom("10.0.0.5", "198.51.100.23"));

        assertThat(remoteAddress(applied)).isEqualTo("198.51.100.23");
    }

    @Test
    void ignoresHeaderFromUntrustedClient() {
        ServerHttpRequest applied = transformer.apply(requestFrom("192.168.1.10", "10.0.0.5"));

        assertThat(remoteAddress(applied)).isEqualTo("192.168.1.10");
    }

    @Test
    void keepsConnectionWithoutHeader() {
        ServerHttpRequest applied = transformer.apply(requestFrom("10.0.0.5"));

        assertThat(remoteAddress(applied)).isEqualTo("10.0.0.5");
    }

    @Test
    void keepsConnectionPort() {
        ServerHttpRequest applied = transformer.apply(requestFrom("10.0.0.5", "198.51.100.23"));

        assertThat(applied.getRemoteAddress().getPort()).isEqualTo(51000);
    }

    // the other forwarded headers are still applied to the request, and hidden
    @Test
    void appliesForwardedHostAndProto() {
        MockServerHttpRequest request = MockServerHttpRequest.get("http://gateway:8080/wms")
                .remoteAddress(new InetSocketAddress("192.168.1.10", 51000))
                .header("X-Forwarded-For", "6.6.6.6")
                .header("X-Forwarded-Host", "geoserver.example.com")
                .header("X-Forwarded-Proto", "https")
                .build();

        ServerHttpRequest applied = transformer.apply(request);

        assertThat(applied.getURI().getScheme()).isEqualTo("https");
        assertThat(applied.getURI().getHost()).isEqualTo("geoserver.example.com");
        assertThat(applied.getHeaders().containsKey("X-Forwarded-For")).isFalse();
        assertThat(remoteAddress(applied)).isEqualTo("192.168.1.10");
    }

    @Test
    void warnsOnceAboutUntrustedHeader(CapturedOutput output) {
        transformer.apply(requestFrom("10.0.0.5", "198.51.100.23"));
        transformer.apply(requestFrom("192.168.1.10"));
        assertThat(output).doesNotContain("not a trusted proxy");

        transformer.apply(requestFrom("192.168.1.10", "6.6.6.6"));
        transformer.apply(requestFrom("192.168.1.11", "6.6.6.7"));
        assertThat(output).containsOnlyOnce("not a trusted proxy").contains("192.168.1.10");
    }

    private static MockServerHttpRequest requestFrom(String connectionAddress, String... forwardedFor) {
        MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.get("http://gateway:8080/wms")
                .remoteAddress(new InetSocketAddress(connectionAddress, 51000));
        for (String value : forwardedFor) {
            request.header("X-Forwarded-For", value);
        }
        return request.build();
    }

    private static String remoteAddress(ServerHttpRequest request) {
        return request.getRemoteAddress().getAddress().getHostAddress();
    }
}
