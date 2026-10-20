/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gateway.filter;

import com.google.common.net.InetAddresses;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.lang.Nullable;

/**
 * The reverse proxies in front of the gateway, and the client address of the requests forwarded by them.
 *
 * <p>A proxy is trusted when its address matches the regular expression of {@code geoserver.gateway.trusted-proxies}.
 * The check uses the address of the connection, never an address from a request header. By default, no proxy is
 * trusted.
 *
 * <p>The client address of a request is the address of its connection. If a trusted proxy opened the connection, the
 * client address comes from the {@code X-Forwarded-For} header written by the proxy, see
 * {@link #clientAddress(String, List)}.
 */
public final class TrustedProxies {

    private final @Nullable Pattern addresses;

    private TrustedProxies(@Nullable Pattern addresses) {
        this.addresses = addresses;
    }

    /** @param addresses the regular expression matching the addresses of the trusted proxies, or null to trust none */
    public static TrustedProxies of(@Nullable Pattern addresses) {
        return new TrustedProxies(addresses);
    }

    public static TrustedProxies none() {
        return new TrustedProxies(null);
    }

    /** Whether the address is one of the trusted proxies. The whole address must match the regular expression. */
    public boolean isTrusted(String address) {
        return addresses != null && addresses.matcher(address).matches();
    }

    /**
     * The address of the client, given the address of the connection and the {@code X-Forwarded-For} headers of the
     * request.
     *
     * <p>Returns the connection address, unless a trusted proxy opened the connection. Each proxy appends the address
     * of its own client to the header. The walk goes from the right-most hop to the left, and returns the first hop
     * that is not a trusted proxy: the hops on its left were written by that untrusted hop, and cannot be trusted. A
     * hop with a port, or with IPv6 brackets, is reduced to its address.
     *
     * <p>The walk also stops at a hop that is not an IP address, and returns the previous hop. A loopback hop of a
     * trusted proxy names the host of the proxy, not a client, and is skipped. A loopback hop that is not trusted stops
     * the walk too: the hop on its left may come from a process on that host.
     */
    public String clientAddress(String connectionAddress, List<String> forwardedForHeaders) {
        if (!isTrusted(connectionAddress)) {
            return connectionAddress;
        }
        List<String> hops = hops(forwardedForHeaders);
        String client = connectionAddress;
        for (int i = hops.size() - 1; i >= 0; i--) {
            Hop hop = Hop.parse(hops.get(i));
            if (hop == null) {
                break;
            }
            boolean trustedHop = isTrusted(hop.address());
            if (hop.loopback() && !trustedHop) {
                break;
            }
            if (hop.loopback()) {
                continue;
            }
            client = hop.address();
            if (!trustedHop) {
                break;
            }
        }
        return client;
    }

    /** The hops of the headers, in order, as a chain of proxies writes them: one header per proxy, or one value */
    private static List<String> hops(List<String> forwardedForHeaders) {
        List<String> hops = new ArrayList<>();
        for (String header : forwardedForHeaders) {
            for (String hop : header.split(",")) {
                String trimmed = hop.trim();
                if (!trimmed.isEmpty()) {
                    hops.add(trimmed);
                }
            }
        }
        return hops;
    }

    /** A hop of the {@code X-Forwarded-For} header, reduced to its IP address */
    private record Hop(String address, boolean loopback) {

        /** @return the hop, or null when the hop is not an IP address. Parses literals only, never does a DNS lookup */
        static @Nullable Hop parse(String hop) {
            String address = stripPortAndBrackets(hop);
            if (!InetAddresses.isInetAddress(address)) {
                return null;
            }
            return new Hop(address, InetAddresses.forString(address).isLoopbackAddress());
        }

        private static String stripPortAndBrackets(String hop) {
            int closingBracket = hop.indexOf(']');
            if (hop.startsWith("[") && closingBracket > 0) {
                return hop.substring(1, closingBracket);
            }
            int colon = hop.indexOf(':');
            boolean ipv4WithPort = colon > 0 && colon == hop.lastIndexOf(':');
            if (ipv4WithPort) {
                return hop.substring(0, colon);
            }
            return hop;
        }
    }
}
