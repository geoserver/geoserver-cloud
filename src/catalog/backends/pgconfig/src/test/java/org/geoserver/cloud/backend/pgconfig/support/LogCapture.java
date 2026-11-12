/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.backend.pgconfig.support;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 * Keeps the events of a logger in memory during a test, down to {@code DEBUG}, and keeps them off the console.
 *
 * <p>Spring Boot's {@code OutputCaptureExtension} writes the captured output to the console as well: a test asserting a
 * warning would still print it.
 */
public final class LogCapture implements AutoCloseable {

    private final Logger logger;
    private final Level level;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    private LogCapture(Logger logger) {
        this.logger = logger;
        this.level = logger.getLevel();
    }

    /** Starts keeping the events of the logger, and stops passing them to the appenders of the parent loggers. */
    public static LogCapture of(String loggerName) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
        LogCapture capture = new LogCapture(logger);
        capture.start();
        return capture;
    }

    /** The formatted messages of the events logged at the level. */
    public List<String> messages(Level eventLevel) {
        return appender.list.stream()
                .filter(event -> event.getLevel() == eventLevel)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    @Override
    public void close() {
        logger.setAdditive(true);
        logger.setLevel(level);
        logger.detachAppender(appender);
        appender.stop();
    }

    private void start() {
        appender.start();
        logger.addAppender(appender);
        logger.setAdditive(false);
        logger.setLevel(Level.DEBUG);
    }
}
