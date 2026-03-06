/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.app;

import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/**
 * Utility to launch a {@link SpringBootApplication @SpringBootApplication} annotated class, enabling the
 * {@code /actuator/startup} endpoint dependency on {@link BufferingApplicationStartup}.
 */
public class GeoServerApplicationLauncher {

    private GeoServerApplicationLauncher() {
        // utility
    }

    public static void run(Class<?> springBootApplicationClass, String... args) {
        try {
            SpringApplication app = new SpringApplication(springBootApplicationClass);
            // Set the startup implementation and buffer capacity, enables the
            // /actuator/startup endpoint
            app.setApplicationStartup(new BufferingApplicationStartup(2048));
            app.run(args);
        } catch (RuntimeException e) {
            try {
                LoggerFactory.getLogger(GeoServerApplicationLauncher.class).error("Application run failed", e);
            } finally {
                System.exit(-1);
            }
        }
    }
}
