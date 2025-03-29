/*
 * (c) 2020-2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Cloud Gateway application for GeoServer Cloud
 * <p>
 * Using Spring Boot 3.2.x with Spring Cloud 2024.0.1+ for improved
 * reactive context propagation, especially for MDC values in logging.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
