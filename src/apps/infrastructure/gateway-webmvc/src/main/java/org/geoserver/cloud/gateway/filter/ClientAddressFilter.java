/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reports the address of the client as the remote address of the request, as resolved by {@link TrustedProxies}.
 *
 * <p>Spring's {@code ForwardedHeaderFilter} runs first, at {@code HIGHEST_PRECEDENCE}, and takes the remote address
 * from the {@code X-Forwarded-For} header. This filter replaces it with the client address resolved from the connection
 * and the headers of the request as received by the servlet container: the connection address, unless a trusted proxy
 * opened the connection.
 *
 * <p>The gateway then sends the client address to the services in {@code X-Forwarded-For}, as the only hop, and logs it
 * in the {@code http.request.remote-addr} MDC attribute. The services take it as the remote address of the request, for
 * their logs, the control-flow rules, the brute-force prevention and the IP based security rules.
 *
 * <p>A warning is logged once when a client other than a trusted proxy sends the header: a reverse proxy missing from
 * {@code geoserver.gateway.trusted-proxies} makes the requests appear to come from the proxy.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ClientAddressFilter extends OncePerRequestFilter {

    private final TrustedProxies trustedProxies;

    private final AtomicBoolean untrustedHeaderLogged = new AtomicBoolean();

    public ClientAddressFilter(TrustedProxies trustedProxies) {
        this.trustedProxies = trustedProxies;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        warnOnceAboutUntrustedHeader(request);
        String clientAddress = trustedProxies.clientAddress(request);
        chain.doFilter(new ClientAddressRequest(request, clientAddress), response);
    }

    private void warnOnceAboutUntrustedHeader(HttpServletRequest request) {
        if (untrustedHeaderLogged.get() || !trustedProxies.ignoresForwardedFor(request)) {
            return;
        }
        if (untrustedHeaderLogged.compareAndSet(false, true)) {
            log.warn(
                    "Ignoring X-Forwarded-For from {}, not a trusted proxy. If it is the reverse proxy, add its address to geoserver.gateway.trusted-proxies",
                    TrustedProxies.connectionAddress(request));
        }
    }

    /** Reports the client address as the remote address, and leaves the rest of the request alone */
    private static class ClientAddressRequest extends HttpServletRequestWrapper {

        private final String clientAddress;

        ClientAddressRequest(HttpServletRequest request, String clientAddress) {
            super(request);
            this.clientAddress = clientAddress;
        }

        @Override
        public String getRemoteAddr() {
            return clientAddress;
        }

        @Override
        public String getRemoteHost() {
            return clientAddress;
        }
    }
}
