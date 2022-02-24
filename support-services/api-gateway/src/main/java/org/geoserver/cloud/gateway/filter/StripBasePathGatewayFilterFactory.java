/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gateway.filter;

import static com.google.common.base.Preconditions.checkArgument;

import lombok.Data;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory.Config;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * See gateway's issue <a
 * href="https://github.com/spring-cloud/spring-cloud-gateway/issues/1759">#1759</a> "Webflux base
 * path does not work with Path predicates"
 */
public class StripBasePathGatewayFilterFactory
        extends AbstractGatewayFilterFactory<StripBasePathGatewayFilterFactory.PrefixConfig> {

    private StripPrefixGatewayFilterFactory stripPrefix = new StripPrefixGatewayFilterFactory();

    public StripBasePathGatewayFilterFactory() {
        super(PrefixConfig.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("prefix");
    }

    @Override
    public GatewayFilter apply(PrefixConfig config) {
        config.checkPreconditions();
        return (exchange, chain) -> {
            final ServerHttpRequest request = exchange.getRequest();

            final String basePath = config.getPrefix();
            final String path = request.getURI().getRawPath();
            // if (basePath.equals(path)) {
            // return chain.filter(exchange);
            // }

            final int partsToRemove = resolvePartsToStrip(basePath, path);
            if (partsToRemove == 0) {
                return chain.filter(exchange);
            }
            GatewayFilter stripFilter = stripPrefix.apply(newStripPrefixConfig(partsToRemove));
            return stripFilter.filter(exchange, chain);
        };
    }

    private Config newStripPrefixConfig(int partsToRemove) {
        Config config = stripPrefix.newConfig();
        config.setParts(partsToRemove);
        return config;
    }

    private int resolvePartsToStrip(String basePath, String requestPath) {
        if (null == basePath) return 0;
        if (!requestPath.startsWith(basePath)) {
            return 0;
        }
        final int basePathSteps = StringUtils.countOccurrencesOf(basePath, "/");
        boolean isRoot = basePath.equals(requestPath);
        return isRoot ? basePathSteps - 1 : basePathSteps;
    }

    public static @Data class PrefixConfig {
        private String prefix;

        public void checkPreconditions() {
            final String prefix = getPrefix();

            // requireNonNull(prefix, "StripBasePath prefix can't be null");
            if (prefix != null) {
                checkArgument(prefix.startsWith("/"), "StripBasePath prefix must start with /");

                checkArgument(
                        "/".equals(prefix) || !prefix.endsWith("/"),
                        "StripBasePath prefix must not end with /");
            }
        }
    }
}
