/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gateway;

import org.geoserver.cloud.gateway.filter.RouteProfileGatewayFilterFactory;
import org.geoserver.cloud.gateway.filter.StripBasePathGatewayFilterFactory;
import org.geoserver.cloud.gateway.predicate.RegExpQueryRoutePredicateFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
public class GatewayApplicationAutoconfiguration {

    /**
     * Custom gateway predicate factory to support matching by regular expressions on both name and
     * value of query parameters
     *
     * <p>E.g.:
     *
     * <pre>{@code
     * - id: wms_ows
     *   uri: http://wms:8080
     *   predicates:
     *     # match service=wms case insensitively
     *     - RegExpQuery=(?i:service),(?i:wms)
     * }</pre>
     */
    @Bean
    RegExpQueryRoutePredicateFactory regExpQueryRoutePredicateFactory() {
        return new RegExpQueryRoutePredicateFactory();
    }

    /**
     * Allows to enable routes only if a given spring profile is enabled
     *
     * <p>Since the `spring.cloud.gateway.routes` is a list and not a map/dictionary, routes can't
     * be added in profiles, because the list is overritten fully. This filter allows to enable
     * routes based on profiles from a single list of routes.
     *
     * <p>E.g.:
     *
     * <pre>{@code
     * - id: catalog
     *   uri: ...
     *   predicates:
     *     - Path=${geoserver.base-path}/api/v1/**
     *   filters:
     *     # Expose the catalog and configuration API only if the dev profile is active
     *     - RouteProfile=dev,403
     * }</pre>
     */
    @Bean
    RouteProfileGatewayFilterFactory routeProfileGatewayFilterFactory(Environment environment) {
        return new RouteProfileGatewayFilterFactory(environment);
    }

    @Bean
    StripBasePathGatewayFilterFactory stripBasePathGatewayFilterFactory() {
        return new StripBasePathGatewayFilterFactory();
    }
}
