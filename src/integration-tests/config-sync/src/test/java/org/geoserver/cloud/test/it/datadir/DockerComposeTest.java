/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.test.it.datadir;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;

import org.geoserver.cloud.autoconfigure.catalog.event.UpdateSequenceController.UpdateSeq;
import org.geoserver.util.IOUtils;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.OutputFrame.OutputType;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Consumer;

@Slf4j(topic = "it.datadir")
@Testcontainers
public abstract class DockerComposeTest {

    private static final int UID = resolveUserID();
    private static final int GID = resolveUserGID();
    private static final String GS_USER = String.format("%d:%d", UID, GID);

    private static @TempDir Path TMP_DATA_DIRECTORY;

    public static class GsCloudComposeContainer
            extends DockerComposeContainer<GsCloudComposeContainer> {

        public GsCloudComposeContainer(File composeFile) {
            super(composeFile);
        }

        @Override
        public void start() {
            String datadir = TMP_DATA_DIRECTORY.toAbsolutePath().toString();
            super.withEnv("TMP_DATADIR", datadir);
            log.info("#################################################");
            log.info("######### {} #######", datadir);
            log.info("#################################################");

            super.start();
        }
    }

    @Container // see https://www.testcontainers.org/modules/docker_compose/
    public static DockerComposeContainer<?> environment =
            new GsCloudComposeContainer(new File("docker-compose-it.yml"))
                    // use the host's docker compose container environment to use the local images
                    // instead of pulling them
                    .withLocalCompose(true) //
                    // run containers with the local user's id and gid
                    .withEnv("GS_USER", GS_USER) //
                    // .withEnv("TMP_DATADIR", TMP_DATA_DIRECTORY.toAbsolutePath().toString())
                    //
                    // fail if images are not in the local registry
                    //
                    .withPull(false) //
                    //
                    // respect resource limits set in the compose file
                    //
                    .withOptions("--compatibility") //
                    //
                    // GEOSERVER_DATA_DIR
                    //
                    // .withEnv("GEOSERVER_DATA_DIR", "")
                    ////////// RabbitMQ
                    .withExposedService(
                            "rabbitmq",
                            5672,
                            Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30))) //
                    // .withLogConsumer("rabbitmq", logConsumer("rabbitmq")) //
                    ////////// Gateway
                    .withExposedService(
                            "gateway",
                            8080,
                            Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(1))) //
                    .withExposedService("gateway", 8081) // management
                    .withLogConsumer("gateway", logConsumer("gateway")) //
                    ////////// WFS
                    .withExposedService(
                            "wfs",
                            8080,
                            Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(1))) //
                    .withLogConsumer("wfs", logConsumer("wfs")) //
                    .withExposedService("wfs", 8081) // management
                    ////////// WMS
                    .withExposedService(
                            "wms",
                            8080,
                            Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(1))) //
                    .withExposedService("wms", 8081) // management
                    .withLogConsumer("wms", logConsumer("wms")) //
                    ////////// WEUBUI
                    .withExposedService(
                            "webui",
                            8080,
                            Wait.forHealthcheck().withStartupTimeout(Duration.ofMinutes(1))) //
                    .withExposedService("webui", 8081) // management
                    .withLogConsumer("webui", logConsumer("webui")) //
            ;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {

        String rabbitHost = environment.getServiceHost("rabbitmq", 5672);
        Integer rabbitPort = environment.getServicePort("rabbitmq", 5672);

        registry.add("spring.rabbitmq.host", () -> rabbitHost);
        registry.add("spring.rabbitmq.port", () -> rabbitPort);

        String datadir = TMP_DATA_DIRECTORY.toAbsolutePath().toString();
        registry.add("geoserver.backend.data-directory.location", () -> datadir);
        log.info("GEOSERVER_DATA_DIRECTORY: {}", TMP_DATA_DIRECTORY);
    }

    private static Consumer<OutputFrame> logConsumer(String serviceName) {
        return frame -> {
            if (frame.getType() == OutputType.END) {
                System.out.printf("\t############## %s: Terminated ##############%n", serviceName);
            } else {
                System.out.printf("\t%s: %s", serviceName, frame.getUtf8String());
            }
        };
    }

    protected UpdateSeq getServiceUpdateSequence(String service) {

        RestTemplate restTemplate = new RestTemplate();
        String updateSeqURL = getServiceURL(service) + "/wfs/admin/updatesequence";
        ResponseEntity<UpdateSeq> response =
                restTemplate.getForEntity(updateSeqURL, UpdateSeq.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.hasBody()).isTrue();

        return response.getBody();
    }

    protected static String getServiceURL(String service) {
        return getServiceURL(service, 8080);
    }

    protected static String getServiceManagementURL(String service) {
        return getServiceURL(service, 8081);
    }

    protected static String getServiceURL(String service, int port) {
        String host = environment.getServiceHost(service, port);
        Integer exportedPort = environment.getServicePort(service, port);
        return buildURL(host, exportedPort);
    }

    protected static String buildURL(String host, Integer port) {
        return String.format("http://%s:%d", host, port);
    }

    private static int resolveUserID() {
        try {
            Process proc = Runtime.getRuntime().exec("id -u");
            String out = IOUtils.toString(proc.getInputStream());
            int id = Integer.parseInt(out.replaceAll("\n", ""));
            return id;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static int resolveUserGID() {
        try {
            Process proc = Runtime.getRuntime().exec("id -g");
            String out = IOUtils.toString(proc.getInputStream());
            int id = Integer.parseInt(out.replaceAll("\n", ""));
            return id;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
