/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.app;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

/**
 * Utility to launch a {@link SpringBootApplication @SpringBootApplication} annotated class, enabling the
 * {@code /actuator/startup} endpoint dependency on {@link BufferingApplicationStartup}.
 */
public class GeoServerApplicationLauncher {

    private GeoServerApplicationLauncher() {
        // utility
    }

    public static void run(Class<?> springBootApplicationClass, String... args) {
        run(springBootApplicationClass, List.of(), args);
    }

    /**
     * Launches the application with additional active profiles.
     *
     * <p>The profiles are added to the profiles of the environment, they do not replace them.
     */
    public static void run(Class<?> springBootApplicationClass, List<String> additionalProfiles, String... args) {
        Logger log = LoggerFactory.getLogger(springBootApplicationClass);
        try {
            SpringApplication app = new SpringApplication(springBootApplicationClass);
            app.setAdditionalProfiles(additionalProfiles.toArray(String[]::new));
            // Set the startup implementation and buffer capacity, enables the
            // /actuator/startup endpoint
            app.setApplicationStartup(new BufferingApplicationStartup(2048));

            // Centralized fix: Add a listener for context closure so the app exits on POST /actuator/shutdown
            app.addListeners((ApplicationListener<ContextClosedEvent>) _ -> scheduleExit());

            app.run(args);
        } catch (RuntimeException e) {
            try {
                log.error("Application run failed", e);
            } finally {
                System.exit(-1);
            }
        }
    }

    @SuppressWarnings("java:S2095")
    private static void scheduleExit() {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> System.exit(0), 5, TimeUnit.SECONDS);
    }
}
