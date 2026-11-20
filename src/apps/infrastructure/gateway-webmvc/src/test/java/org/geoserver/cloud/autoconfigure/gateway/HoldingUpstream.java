/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Upstream server for the gateway tests. It can hold incoming requests until the test releases them, and it records how
 * many requests it held at once. It also records the client port of each request: the port identifies the connection of
 * the gateway pool used by the request.
 */
class HoldingUpstream implements AutoCloseable {

    private final HttpServer server;

    private final AtomicInteger inFlight = new AtomicInteger();

    private final AtomicInteger maxInFlight = new AtomicInteger();

    private final List<Integer> clientPorts = new CopyOnWriteArrayList<>();

    private volatile CountDownLatch release = new CountDownLatch(0);

    private HoldingUpstream(HttpServer server) {
        this.server = server;
    }

    static HoldingUpstream start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 100);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        HoldingUpstream upstream = new HoldingUpstream(server);
        server.createContext("/", upstream::handle);
        server.start();
        return upstream;
    }

    String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    /** Holds the next requests until {@link #releaseRequests()}, and starts counting them from zero. */
    void holdRequests() {
        reset();
        release = new CountDownLatch(1);
    }

    void releaseRequests() {
        release.countDown();
    }

    void reset() {
        maxInFlight.set(0);
        clientPorts.clear();
    }

    int inFlight() {
        return inFlight.get();
    }

    int maxInFlight() {
        return maxInFlight.get();
    }

    List<Integer> clientPorts() {
        return List.copyOf(clientPorts);
    }

    @Override
    public void close() {
        releaseRequests();
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        clientPorts.add(exchange.getRemoteAddress().getPort());
        int current = inFlight.incrementAndGet();
        maxInFlight.accumulateAndGet(current, Math::max);
        try {
            awaitRelease();
            respond(exchange);
        } finally {
            inFlight.decrementAndGet();
            exchange.close();
        }
    }

    private void awaitRelease() {
        try {
            release.await();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    private static void respond(HttpExchange exchange) throws IOException {
        byte[] body = "released".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
