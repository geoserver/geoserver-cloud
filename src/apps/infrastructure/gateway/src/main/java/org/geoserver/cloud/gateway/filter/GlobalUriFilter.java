/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * See gateway's issue <a
 * href="https://github.com/spring-cloud/spring-cloud-gateway/issues/2065">#2065</a> "Double Encoded URLs"
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

			// Use the original incomingUri path and query params
			final var routeUri = route.getUri();
			URI mergedUri = createUri(incomingUri, routeUri);

			// Save it as the outgoing URI to call the service, and override the "wrongly" double encoded URI
			exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, mergedUri);
		}

		return chain.filter(exchange);
	}

	private URI createUri(URI incomingUri, URI routeUri) {
		final var port = routeUri.getPort() != -1 ? ":" + routeUri.getPort() : "";
		final var rawPath = incomingUri.getRawPath() != null ? incomingUri.getRawPath() : "";
		final var query = incomingUri.getRawQuery() != null ?  "?" + incomingUri.getRawQuery() : "";
		return URI.create(routeUri.getScheme() + "://" + routeUri.getHost() + port + rawPath + query);
	}

	private static boolean isUriEncoded(URI uri) {
		return (uri.getRawQuery() != null && uri.getRawQuery().contains("%"))
			|| (uri.getRawPath() != null && uri.getRawPath().contains("%"));
	}

	@Override
	public int getOrder() {
		return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
	}
}
