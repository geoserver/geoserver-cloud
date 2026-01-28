/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gateway.logging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

/**
 * Simple test application to verify MDC logging.
 * <p>
 * Run with:
 * <pre>
 * cd gateway
 * mvn spring-boot:run -Dspring-boot.run.profiles=json-logs -Dspring-boot.run.main-class=org.geoserver.cloud.gateway.logging.MdcPropagationTestCommand
 * </pre>
 */
@SpringBootApplication
@Slf4j
public class MdcPropagationTestCommand {

    public static void main(String[] args) {
        new SpringApplicationBuilder(MdcPropagationTestCommand.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Bean
    public CommandLineRunner testMdcLogging() {
        return _ -> {
            // Log without MDC
            log.info("Logging without MDC");

            // Set MDC values
            MDC.put("test.id", "test-123");
            MDC.put("test.path", "/api/test");
            MDC.put("test.method", "GET");

            // Log with MDC
            log.info("Logging with MDC set");

            // Clear MDC
            MDC.clear();

            log.info("Test complete");
        };
    }
}
