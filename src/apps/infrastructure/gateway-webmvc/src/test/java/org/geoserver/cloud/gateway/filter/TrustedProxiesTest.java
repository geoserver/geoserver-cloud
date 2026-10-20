/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class TrustedProxiesTest {

    /** The private ranges of the test addresses, as trusted by default by Tomcat's RemoteIpValve */
    private static final TrustedProxies PRIVATE =
            TrustedProxies.of(Pattern.compile("(10|127|172\\.16|192\\.168)\\..*"));

    private static final TrustedProxies LOOPBACK = TrustedProxies.of(Pattern.compile("127\\..*|::1"));

    @Test
    void matchesWholeAddress() {
        TrustedProxies proxies = TrustedProxies.of(Pattern.compile("10\\.0\\.0\\.\\d+"));

        assertThat(proxies.isTrusted("10.0.0.5")).isTrue();
        assertThat(proxies.isTrusted("110.0.0.5")).isFalse();
        assertThat(proxies.isTrusted("10.0.0.5.1")).isFalse();
    }

    @Test
    void trustsNoProxyByDefault() {
        TrustedProxies none = TrustedProxies.none();

        assertThat(none.isTrusted("127.0.0.1")).isFalse();
        assertThat(none.clientAddress("127.0.0.1", List.of("198.51.100.23"))).isEqualTo("127.0.0.1");
    }

    @Test
    void untrustedConnectionIgnoresHeader() {
        assertThat(PRIVATE.clientAddress("203.0.113.7", List.of("10.0.0.5"))).isEqualTo("203.0.113.7");
    }

    @Test
    void noHeaderKeepsConnection() {
        assertThat(PRIVATE.clientAddress("10.0.0.1", List.of())).isEqualTo("10.0.0.1");
    }

    @Test
    void trustedConnectionTakesClient() {
        assertThat(PRIVATE.clientAddress("10.0.0.1", List.of("198.51.100.23"))).isEqualTo("198.51.100.23");
    }

    // the client is the right-most hop that is not a trusted proxy
    @Test
    void rightmostUntrustedHop() {
        assertThat(PRIVATE.clientAddress("10.0.0.1", List.of("198.51.100.23, 172.16.0.9")))
                .isEqualTo("198.51.100.23");
    }

    @Test
    void allHopsTrustedTakesLeftmost() {
        assertThat(PRIVATE.clientAddress("10.0.0.1", List.of("192.168.1.5, 172.16.0.9")))
                .isEqualTo("192.168.1.5");
    }

    // two proxies on the same host: the loopback hop is the first proxy, the client is on its left
    @Test
    void loopbackHopSkipped() {
        assertThat(LOOPBACK.clientAddress("127.0.0.1", List.of("198.51.100.23, 127.0.0.1")))
                .isEqualTo("198.51.100.23");
    }

    // a request made on the proxy host itself comes from the proxy
    @Test
    void loopbackOnlyHeaderKeepsConnection() {
        assertThat(PRIVATE.clientAddress("10.0.0.1", List.of("127.0.0.1"))).isEqualTo("10.0.0.1");
        assertThat(PRIVATE.clientAddress("10.0.0.1", List.of("::1"))).isEqualTo("10.0.0.1");
        assertThat(PRIVATE.clientAddress("10.0.0.1", List.of("127.0.0.1, 127.0.0.2")))
                .isEqualTo("10.0.0.1");
    }

    // loopback is not trusted here, so the hop on its left may come from a local process
    @Test
    void untrustedLoopbackHopStopsWalk() {
        TrustedProxies proxies = TrustedProxies.of(Pattern.compile("10\\.1\\.2\\.3"));

        assertThat(proxies.clientAddress("10.1.2.3", List.of("198.51.100.23, 127.0.0.1")))
                .isEqualTo("10.1.2.3");
    }

    @Test
    void portAndBracketsStripped() {
        assertThat(PRIVATE.clientAddress("10.0.0.1", List.of("198.51.100.23:5555")))
                .isEqualTo("198.51.100.23");
        assertThat(PRIVATE.clientAddress("10.0.0.1", List.of("[2001:db8::1]:1234")))
                .isEqualTo("2001:db8::1");
        assertThat(PRIVATE.clientAddress("10.0.0.1", List.of("[2001:db8::1]"))).isEqualTo("2001:db8::1");
        assertThat(PRIVATE.clientAddress("10.0.0.1", List.of("2001:db8::1"))).isEqualTo("2001:db8::1");
    }

    @Test
    void invalidHopStopsAtLastTrustedHop() {
        assertThat(PRIVATE.clientAddress("10.0.0.1", List.of("198.51.100.23, unknown")))
                .isEqualTo("10.0.0.1");
        assertThat(PRIVATE.clientAddress("10.0.0.1", List.of("198.51.100.23, proxy.example, 172.16.0.9")))
                .isEqualTo("172.16.0.9");
    }

    @Test
    void multipleHeadersMerged() {
        assertThat(PRIVATE.clientAddress("10.0.0.1", List.of("198.51.100.23", "172.16.0.9")))
                .isEqualTo("198.51.100.23");
    }

    @Test
    void blankTokensIgnored() {
        assertThat(PRIVATE.clientAddress("10.0.0.1", List.of("198.51.100.23, ")))
                .isEqualTo("198.51.100.23");
    }
}
