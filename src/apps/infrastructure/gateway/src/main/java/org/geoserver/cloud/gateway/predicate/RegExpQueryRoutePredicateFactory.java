/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gateway.predicate;

import jakarta.validation.constraints.NotEmpty;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.GatewayPredicate;
import org.springframework.cloud.gateway.handler.predicate.QueryRoutePredicateFactory;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ServerWebExchange;

/**
 * Gateway predicate factory that allows matching by HTTP request query string parameters using
 * {@link Pattern Java regular expressions} for both parameter name and value.
 *
 * <p>This predicate factory is similar to the {@link QueryRoutePredicateFactory} but besides
 * allowing regular expressions to match a parameter value, also allows to match the parameter name
 * through a regex.
 *
 * <p>Just like with {@link QueryRoutePredicateFactory}, the "value" regular expression is optional.
 * If not given, the test will be performed only against the query parameter names. If a value
 * regular expression is present, though, the evaluation will be performed against the values of the
 * first parameter that matches the name regex.
 *
 * <p>Sample usage: the following route configuration example uses a {@code RegExpQuery} predicate
 * to match the {@code service} query parameter name and {@code wfs} value in a case insensitive
 * fashion.
 *
 * <pre>
 * <code>
 * spring:
 *  cloud:
 *   gateway:
 *    routes:
 *     - id: wfs_ows
 *       uri: lb://wfs-service
 *       predicates:
 *       - RegExpQuery=(?i:service),(?i:wfs)
 * </code>
 * </pre>
 */
public class RegExpQueryRoutePredicateFactory
        extends AbstractRoutePredicateFactory<RegExpQueryRoutePredicateFactory.Config> {

    /** HTTP request query parameter regexp key. */
    public static final String PARAM_KEY = "paramRegexp";

    /** HTTP request query parameter value regexp key. */
    public static final String VALUE_KEY = "valueRegexp";

    public RegExpQueryRoutePredicateFactory() {
        super(Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList(PARAM_KEY, VALUE_KEY);
    }

    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        return new RegExpQueryRoutePredicate(config);
    }

    @RequiredArgsConstructor
    private static class RegExpQueryRoutePredicate implements GatewayPredicate {
        private final @NonNull Config config;

        @Override
        public boolean test(ServerWebExchange exchange) {
            final String paramRegexp = config.getParamRegexp();
            final String valueRegexp = config.getValueRegexp();

            boolean matchNameOnly = !StringUtils.hasText(config.getValueRegexp());
            Optional<String> paramName = findParameterName(paramRegexp, exchange);
            boolean paramNameMatches = paramName.isPresent();
            if (matchNameOnly) {
                return paramNameMatches;
            }
            return paramNameMatches && paramValueMatches(paramName.get(), valueRegexp, exchange);
        }

        @Override
        public String toString() {
            return "Query: param regexp='%s' value regexp='%s'"
                    .formatted(config.getParamRegexp(), config.getValueRegexp());
        }
    }

    static Optional<String> findParameterName(@NonNull String regex, ServerWebExchange exchange) {
        Set<String> parameterNames = exchange.getRequest().getQueryParams().keySet();
        return parameterNames.stream().filter(name -> name.matches(regex)).findFirst();
    }

    static boolean paramValueMatches(
            @NonNull String paramName, @NonNull String valueRegEx, ServerWebExchange exchange) {
        List<String> values = exchange.getRequest().getQueryParams().get(paramName);
        return values != null && values.stream().anyMatch(v -> v != null && v.matches(valueRegEx));
    }

    @Data
    @Accessors(chain = true)
    @Validated
    public static class Config {

        @NotEmpty
        private String paramRegexp;

        private String valueRegexp;
    }
}
