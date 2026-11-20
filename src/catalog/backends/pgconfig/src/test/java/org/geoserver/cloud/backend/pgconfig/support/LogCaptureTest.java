/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.support;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LogCaptureTest {

    private static final String LOGGER_NAME = LogCaptureTest.class.getName();

    // tests running concurrently log through the captured logger while a test reads its messages
    @Test
    void readsWhileAnotherThreadLogs() {
        Logger logger = LoggerFactory.getLogger(LOGGER_NAME);
        AtomicBoolean logging = new AtomicBoolean(true);
        try (LogCapture capture = LogCapture.of(LOGGER_NAME)) {
            CompletableFuture<Void> writer = CompletableFuture.runAsync(() -> {
                while (logging.get()) {
                    logger.debug("logged by another thread");
                }
            });
            try {
                for (int read = 0; read < 1_000; read++) {
                    capture.messages(Level.DEBUG);
                }
            } finally {
                logging.set(false);
                writer.join();
            }
            assertThat(capture.messages(Level.DEBUG)).isNotEmpty().allMatch("logged by another thread"::equals);
        }
    }
}
