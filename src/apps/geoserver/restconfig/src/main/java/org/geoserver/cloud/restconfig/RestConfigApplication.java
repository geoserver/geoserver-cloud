/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.restconfig;

import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class RestConfigApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(RestConfigApplication.class, args);
        } catch (RuntimeException e) {
            try {
                LoggerFactory.getLogger(RestConfigApplication.class).error("Application run failed", e);
            } finally {
                System.exit(-1);
            }
        }
    }
}
