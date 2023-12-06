/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gateway;

import org.geoserver.cloud.gateway.filter.RouteProfileGatewayFilterFactory;
import org.geoserver.cloud.gateway.filter.StripBasePathGatewayFilterFactory;
import org.geoserver.cloud.gateway.predicate.RegExpQueryRoutePredicateFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@SpringBootApplication
@Configuration(proxyBeanMethods = false)
public class GatewayApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(GatewayApplication.class).run(args);
    }

    @Bean
    RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes().build();
    }

    /**
     * Custom gateway predicate factory to support matching by regular expressions on both name and
     * value of query parameters
     */
    @Bean
    RegExpQueryRoutePredicateFactory regExpQueryRoutePredicateFactory() {
        return new RegExpQueryRoutePredicateFactory();
    }

    /** Allows to enable routes only if a given spring profile is enabled */
    @Bean
    RouteProfileGatewayFilterFactory routeProfileGatewayFilterFactory(Environment environment) {
        return new RouteProfileGatewayFilterFactory(environment);
    }

    @Bean
    StripBasePathGatewayFilterFactory stripBasePathGatewayFilterFactory() {
        return new StripBasePathGatewayFilterFactory();
    }
}
