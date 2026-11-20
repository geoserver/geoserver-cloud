/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.geoserver.cloud.gateway.app.GatewayMvcApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Verifies the gateway reaches the services through a connection pool configured by {@link HttpClientPoolProperties}.
 * The tests send requests through the gateway to upstream servers, and can make those servers hold the requests.
 *
 * <p>Each upstream server has its own address, hence its own pool in the gateway.
 */
@SpringBootTest(
        classes = GatewayMvcApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.cloud.gateway.server.webmvc.httpclient.pool.max-connections=24",
            "spring.cloud.gateway.server.webmvc.httpclient.pool.acquire-timeout=2s",
            "spring.cloud.gateway.server.webmvc.httpclient.pool.max-idle-time=200ms"
        })
@ActiveProfiles("test")
class GatewayHttpClientAutoConfigurationIT {

    private static HoldingUpstream poolUpstream;

    private static HoldingUpstream idleUpstream;

    @LocalServerPort
    private int port;

    @Autowired
    private MeterRegistry meterRegistry;

    private final HttpClient client =
            HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

    @BeforeAll
    static void startUpstreams() throws IOException {
        poolUpstream = HoldingUpstream.start();
        idleUpstream = HoldingUpstream.start();
    }

    @AfterAll
    static void stopUpstreams() {
        poolUpstream.close();
        idleUpstream.close();
    }

    @DynamicPropertySource
    static void registerRoutes(DynamicPropertyRegistry registry) {
        String routes = "spring.cloud.gateway.server.webmvc.routes";
        registry.add(routes + "[0].id", () -> "pool");
        registry.add(routes + "[0].uri", poolUpstream::baseUrl);
        registry.add(routes + "[0].predicates[0]", () -> "Path=/pool/**");
        registry.add(routes + "[1].id", () -> "idle");
        registry.add(routes + "[1].uri", idleUpstream::baseUrl);
        registry.add(routes + "[1].predicates[0]", () -> "Path=/idle/**");
    }

    @AfterEach
    void releaseHeldRequests() {
        poolUpstream.releaseRequests();
        client.close();
    }

    @Test
    void poolSizeCapsUpstreamRequests() {
        poolUpstream.holdRequests();

        List<CompletableFuture<HttpResponse<String>>> responses = sendConcurrently("/pool/held", 32);

        await().until(() -> poolUpstream.inFlight() == 24);
        // the other 8 requests wait in the gateway for a connection
        await().during(Duration.ofMillis(300)).until(() -> poolUpstream.inFlight() == 24);
        poolUpstream.releaseRequests();
        assertThat(statuses(responses)).hasSize(32).containsOnly(200);
        assertThat(poolUpstream.maxInFlight()).isEqualTo(24);
    }

    @Test
    void waitForConnectionTimesOut() {
        poolUpstream.holdRequests();

        List<CompletableFuture<HttpResponse<String>>> responses = sendConcurrently("/pool/held", 32);

        // the requests above the pool size give up while the upstream still holds the other ones
        await().atMost(Duration.ofSeconds(10)).until(() -> completed(responses) == 8);
        poolUpstream.releaseRequests();
        List<Integer> statuses = statuses(responses);
        assertThat(statuses).filteredOn(status -> status == 200).hasSize(24);
        assertThat(statuses).filteredOn(status -> status >= 500).hasSize(8);
    }

    // the wait is the point: the connection must sit idle past the max idle time of the pool
    @SuppressWarnings("java:S2925")
    @Test
    void idleConnectionIsNotReused() throws Exception {
        idleUpstream.reset();

        send("/idle/first");
        Thread.sleep(Duration.ofMillis(600));
        send("/idle/second");

        // a second request on the same connection would come from the same client port
        assertThat(idleUpstream.clientPorts()).hasSize(2).doesNotHaveDuplicates();
    }

    @Test
    void publishesPoolMetrics() throws Exception {
        send("/pool/metrics");

        Gauge maxConnections = meterRegistry
                .find("reactor.netty.connection.provider.max.connections")
                .tag("name", "proxy")
                .gauge();
        assertThat(maxConnections).isNotNull();
        assertThat(maxConnections.value()).isEqualTo(24);
    }

    private List<CompletableFuture<HttpResponse<String>>> sendConcurrently(String path, int count) {
        List<CompletableFuture<HttpResponse<String>>> responses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            responses.add(client.sendAsync(request(path), HttpResponse.BodyHandlers.ofString()));
        }
        return responses;
    }

    private void send(String path) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(request(path), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private HttpRequest request(String path) {
        URI uri = URI.create("http://localhost:" + port + path);
        return HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).build();
    }

    private static long completed(List<CompletableFuture<HttpResponse<String>>> responses) {
        return responses.stream().filter(CompletableFuture::isDone).count();
    }

    private static List<Integer> statuses(List<CompletableFuture<HttpResponse<String>>> responses) {
        return responses.stream()
                .map(CompletableFuture::join)
                .map(HttpResponse::statusCode)
                .toList();
    }
}
