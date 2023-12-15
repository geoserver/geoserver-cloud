/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.gwc.app;

import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GeoWebCacheApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(GeoWebCacheApplication.class, args);
        } catch (RuntimeException e) {
            try {
                LoggerFactory.getLogger(GeoWebCacheApplication.class)
                        .error("Application run failed", e);
            } finally {
                System.exit(-1);
            }
        }
    }
}
