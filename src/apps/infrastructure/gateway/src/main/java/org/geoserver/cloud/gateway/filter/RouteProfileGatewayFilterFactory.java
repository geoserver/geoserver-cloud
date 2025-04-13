/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gateway.filter;

import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;

import jakarta.validation.constraints.NotEmpty;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Allows to enable routes only if a given spring profile is enabled */
public class RouteProfileGatewayFilterFactory
        extends AbstractGatewayFilterFactory<RouteProfileGatewayFilterFactory.Config> {

    private static final List<String> SHORTCUT_FIELD_ORDER =
            Collections.unmodifiableList(Arrays.asList(Config.PROFILE_KEY, Config.HTTPSTATUS_KEY));

    private final Environment environment;

    public RouteProfileGatewayFilterFactory(@NonNull Environment environment) {
        super(Config.class);
        this.environment = environment;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return SHORTCUT_FIELD_ORDER;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new RouteProfileGatewayFilter(environment, config);
    }

    @RequiredArgsConstructor
    private static class RouteProfileGatewayFilter implements GatewayFilter {

        private final @NonNull Environment environment;
        private final @NonNull Config config;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            final List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
            String profile = config.getProfile();
            if (StringUtils.hasText(profile)) {
                final boolean exclude = profile.startsWith("!");
                profile = exclude ? profile.substring(1) : profile;

                boolean profileMatch = activeProfiles.contains(profile);
                boolean proceed = (profileMatch && !exclude) || (!profileMatch && exclude);
                if (proceed) {
                    // continue...
                    return chain.filter(exchange);
                }
            }

            int status = config.getStatusCode();
            exchange.getResponse().setRawStatusCode(status);
            return exchange.getResponse().setComplete();
        }

        @Override
        public String toString() {
            return filterToStringCreator(this)
                    .append(Config.PROFILE_KEY, config.getProfile())
                    .append(Config.HTTPSTATUS_KEY, config.getStatusCode())
                    .toString();
        }
    }

    @Data
    @Accessors(chain = true)
    @Validated
    public static class Config {

        /**
         * Profiles key, indicates which profiles must be enabled to allow the request to proceed
         */
        public static final String PROFILE_KEY = "profile";

        /**
         * Status code key. HTTP status code to return when the request is not allowed to proceed
         * because the required profiles are not active
         */
        public static final String HTTPSTATUS_KEY = "statusCode";

        @NotEmpty
        private String profile;

        private int statusCode = HttpStatus.NOT_FOUND.value();
    }
}
