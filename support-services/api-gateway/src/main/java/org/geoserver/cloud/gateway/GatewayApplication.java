/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gateway;

import lombok.extern.slf4j.Slf4j;
import org.geoserver.cloud.gateway.filter.RouteProfileGatewayFilterFactory;
import org.geoserver.cloud.gateway.filter.StripBasePathGatewayFilterFactory;
import org.geoserver.cloud.gateway.predicate.RegExpQueryRoutePredicateFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.unit.DataSize;

@Slf4j
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(GatewayApplication.class).run(args);
    }

    public @Bean RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes().build();
    }

    /**
     * Custom gateway predicate factory to support matching by regular expressions on both name and
     * value of query parameters
     */
    public @Bean RegExpQueryRoutePredicateFactory regExpQueryRoutePredicateFactory() {
        return new RegExpQueryRoutePredicateFactory();
    }

    /** Allows to enable routes only if a given spring profile is enabled */
    public @Bean RouteProfileGatewayFilterFactory routeProfileGatewayFilterFactory() {
        return new RouteProfileGatewayFilterFactory();
    }

    public @Bean StripBasePathGatewayFilterFactory stripBasePathGatewayFilterFactory() {
        return new StripBasePathGatewayFilterFactory();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent e) {
        ConfigurableEnvironment env = e.getApplicationContext().getEnvironment();

        String app = env.getProperty("spring.application.name");
        String instanceId = env.getProperty("info.instance-id");
        int cpus = Runtime.getRuntime().availableProcessors();
        String maxMem;
        {
            DataSize maxMemBytes = DataSize.ofBytes(Runtime.getRuntime().maxMemory());
            double value = maxMemBytes.toKilobytes() / 1024d;
            String unit = "MB";
            if (maxMemBytes.toGigabytes() > 0) {
                value = value / 1024d;
                unit = "GB";
            }
            maxMem = String.format("%.2f %s", value, unit);
        }
        log.info(
                "{} ready. Instance-id: {}, cpus: {}, max memory: {}. Running as {}({}:{})",
                app,
                instanceId,
                cpus,
                maxMem,
                env.getProperty("user.name"),
                env.getProperty("user.id"),
                env.getProperty("user.gid"));
    }
}
