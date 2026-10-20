/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gateway.filter;

import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;

/**
 * Applies the forwarded headers to the request like Spring's {@link ForwardedHeaderTransformer}, and sets the remote
 * address of the request to the client address resolved by {@link TrustedProxies}.
 *
 * <p>Spring's transformer takes the remote address from the {@code X-Forwarded-For} header. This one sets it to the
 * address of the connection, unless a trusted proxy opened the connection. The gateway then sends the client address
 * to the services in {@code X-Forwarded-For}, as the only hop. The services take it as the remote address of the
 * request, for their logs, the control-flow rules, the brute-force prevention and the IP based security rules.
 *
 * <p>Registered as the {@code forwardedHeaderTransformer} bean, in place of the one of Spring Boot, when
 * {@code server.forward-headers-strategy} is {@code framework}.
 *
 * <p>A warning is logged once when a client other than a trusted proxy sends the header: a reverse proxy missing from
 * {@code geoserver.gateway.trusted-proxies} makes the requests appear to come from the proxy.
 */
@Slf4j
public class ClientAddressForwardedHeaderTransformer extends ForwardedHeaderTransformer {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final TrustedProxies trustedProxies;

    private final AtomicBoolean untrustedHeaderLogged = new AtomicBoolean();

    public ClientAddressForwardedHeaderTransformer(TrustedProxies trustedProxies) {
        this.trustedProxies = trustedProxies;
    }

    @Override
    public ServerHttpRequest apply(ServerHttpRequest request) {
        ServerHttpRequest applied = super.apply(request);
        InetSocketAddress connection = request.getRemoteAddress();
        if (connection == null || connection.getAddress() == null) {
            return applied;
        }
        List<String> forwardedFor = request.getHeaders().getOrEmpty(FORWARDED_FOR_HEADER);
        warnOnceAboutUntrustedHeader(connection, forwardedFor);
        InetSocketAddress client = clientAddress(connection, forwardedFor);
        return applied.mutate().remoteAddress(client).build();
    }

    private InetSocketAddress clientAddress(InetSocketAddress connection, List<String> forwardedFor) {
        String connectionAddress = connection.getAddress().getHostAddress();
        String clientAddress = trustedProxies.clientAddress(connectionAddress, forwardedFor);
        if (clientAddress.equals(connectionAddress)) {
            return connection;
        }
        // a hop of the header, already validated as an IP literal: parsing it does no DNS lookup
        InetAddress address = InetAddresses.forString(clientAddress);
        return new InetSocketAddress(address, connection.getPort());
    }

    private void warnOnceAboutUntrustedHeader(InetSocketAddress connection, List<String> forwardedFor) {
        if (untrustedHeaderLogged.get() || forwardedFor.isEmpty()) {
            return;
        }
        String connectionAddress = connection.getAddress().getHostAddress();
        if (!trustedProxies.isTrusted(connectionAddress) && untrustedHeaderLogged.compareAndSet(false, true)) {
            log.warn(
                    "Ignoring X-Forwarded-For from {}, not a trusted proxy. If it is the reverse proxy, add its address to geoserver.gateway.trusted-proxies",
                    connectionAddress);
        }
    }
}
