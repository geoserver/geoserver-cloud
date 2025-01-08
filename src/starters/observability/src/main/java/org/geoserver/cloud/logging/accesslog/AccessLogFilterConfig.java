/*
 * (c) 2024 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.logging.accesslog;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration to set white/black list over the request URL to determine if
 * the access log filter will log an entry for it.
 */
@Data
@ConfigurationProperties(prefix = "logging.accesslog")
@Slf4j(topic = "org.geoserver.cloud.accesslog")
public class AccessLogFilterConfig {

    public static final String ENABLED_KEY = "logging.accesslog.enabled";

    /**
     * A list of java regular expressions applied to the request URL for logging at
     * trace level
     */
    List<Pattern> trace = new ArrayList<>();

    /**
     * A list of java regular expressions applied to the request URL for logging at
     * debug level
     */
    List<Pattern> debug = new ArrayList<>();

    /**
     * A list of java regular expressions applied to the request URL for logging at
     * info level
     */
    List<Pattern> info = new ArrayList<>();

    private enum Level {
        OFF {
            @Override
            void log(String message, Object... args) {
                // no-op
            }
        },
        TRACE {
            @Override
            void log(String message, Object... args) {
                log.trace(message, args);
            }
        },
        DEBUG {
            @Override
            void log(String message, Object... args) {
                log.debug(message, args);
            }
        },
        INFO {
            @Override
            void log(String message, Object... args) {
                log.info(message, args);
            }
        };

        abstract void log(String message, Object... args);
    }

    public void log(String method, int statusCode, String uri) {
        Level level = getLogLevel(uri);
        level.log("{} {} {} ", method, statusCode, uri);
    }

    Level getLogLevel(String uri) {
        if (log.isInfoEnabled() && matches(uri, info)) return Level.INFO;
        if (log.isDebugEnabled() && matches(uri, debug)) return Level.INFO;
        if (log.isTraceEnabled() && matches(uri, trace)) return Level.INFO;

        return Level.OFF;
    }

    private boolean matches(String url, List<Pattern> patterns) {
        return patterns != null
                && !patterns.isEmpty()
                && patterns.stream().anyMatch(pattern -> pattern.matcher(url).matches());
    }
}
