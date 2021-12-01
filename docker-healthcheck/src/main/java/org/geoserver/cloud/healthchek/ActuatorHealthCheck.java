/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.healthchek;

import ch.qos.logback.classic.Level;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Standalone, command line application to be used as a docker healthcheck over a Spring-boot
 * actuator health end point.
 *
 * <p>Command line arguments can be an unnamed port number of full URL, and a (logback) log {@link
 * Level}: {@code log=<level>}
 *
 * <p>If no port number or target URI is given, defaults to {@literal
 * http://localhost:8080/actuator/health}
 *
 * <p>Examples:
 *
 * <pre>{@code
 * java -Dlog.level=debug -jar gs-cloud-docker-support-<version>-bin.jar
 * java -Dlog.level=debug -jar gs-cloud-docker-support-<version>-bin.jar 9090
 * java -jar gs-cloud-docker-support-<version>-bin.jar 9090
 * java -jar gs-cloud-docker-support-<version>-bin.jar http://localhost:9100/geoserver/actuator/health
 * }</pre>
 */
@Slf4j(topic = "org.geoserver.cloud.healthchek")
@SpringBootApplication
public class ActuatorHealthCheck implements CommandLineRunner, ExitCodeGenerator {

    private static final int DEFAULT_PORT = 8080;

    @Getter(onMethod = @__(@Override))
    private int exitCode = 1;

    public static void main(String[] args) {
        SpringApplication app =
                new SpringApplicationBuilder(ActuatorHealthCheck.class)
                        .logStartupInfo(false)
                        .headless(true)
                        .web(WebApplicationType.NONE)
                        .bannerMode(Mode.OFF)
                        .build();
        System.exit(SpringApplication.exit(app.run(args)));
    }

    @Override
    public void run(String... args) {
        final URI url = buildUrl(args);
        try {
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = new RestTemplate().getForEntity(url, Map.class);
            HttpStatus statusCode = response.getStatusCode();
            if (statusCode.is2xxSuccessful()) {
                exitCode = 0;
                if (log.isDebugEnabled()) {
                    log.debug("healthy ({}): {}: {}", statusCode.value(), url, response.getBody());
                } else if (log.isInfoEnabled()) {
                    log.info("healthy ({}): {}", statusCode.value(), url);
                }
            } else {
                log.info("Not healthy ({}): {}", statusCode, url);
                exitCode = statusCode.value();
            }
        } catch (RestClientException e) {
            log.info("Not healthy ({}): {}", url, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Unexpected error running health check: {}", e.getMessage());
        }
    }

    private static URI buildUrl(String... args) {
        args = Arrays.stream(args).filter(arg -> !arg.startsWith("log=")).toArray(String[]::new);
        int port = DEFAULT_PORT;
        if (args != null && args.length > 0) {
            try {
                log.trace("trying command line argument '{}' as port number...", args[0]);
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException invalidPort) {
                log.trace("trying command line argument '{}' as full URL...", args[0]);
                try {
                    return URI.create(args[0]);
                } catch (IllegalArgumentException invalidURI) {

                }
            }
        }
        return URI.create("http://localhost:" + port + "/actuator/health");
    }
}
