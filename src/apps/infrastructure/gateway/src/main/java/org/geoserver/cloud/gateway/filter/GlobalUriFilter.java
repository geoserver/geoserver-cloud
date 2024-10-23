/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gateway.filter;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

import java.net.URI;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * See gateway's issue <a
 * href="https://github.com/spring-cloud/spring-cloud-gateway/issues/2065">#2065</a> "Double Encoded
 * URLs"
 */
@Component
public class GlobalUriFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        URI incomingUri = exchange.getRequest().getURI();
        if (isUriEncoded(incomingUri)) {
            // Get the original Gateway route (contains the service's original host)
            Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
            if (route == null) {
                return chain.filter(exchange);
            }

            // Save it as the outgoing URI to call the service, and override the "wrongly" double
            // encoded URI
            // in  ReactiveLoadBalancerClientFilter LoadBalancerUriTools::containsEncodedParts
            // double encoded URI again
            URI balanceUrl = exchange.getRequiredAttribute(GATEWAY_REQUEST_URL_ATTR);
            URI mergedUri = createUri(incomingUri, balanceUrl);
            exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, mergedUri);
        }

        return chain.filter(exchange);
    }

    private URI createUri(URI incomingUri, URI balanceUrl) {
        final var port = balanceUrl.getPort() != -1 ? ":" + balanceUrl.getPort() : "";
        final var rawPath = balanceUrl.getRawPath() != null ? balanceUrl.getRawPath() : "";
        final var query = incomingUri.getRawQuery() != null ? "?" + incomingUri.getRawQuery() : "";
        return URI.create(balanceUrl.getScheme() + "://" + balanceUrl.getHost() + port + rawPath + query);
    }

    private static boolean isUriEncoded(URI uri) {
        return (uri.getRawQuery() != null && uri.getRawQuery().contains("%"))
                || (uri.getRawPath() != null && uri.getRawPath().contains("%"));
    }

    // order after ReactiveLoadBalancerClientFilter
    @Override
    public int getOrder() {
        return ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER + 1;
    }
}
