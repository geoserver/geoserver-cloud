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
     * A list of java regular expressions applied to the request URL for logging at trace level.
     * <p>
     * Requests with URLs matching any of these patterns will be logged at TRACE level if
     * trace logging is enabled. These patterns should follow Java's regular expression syntax.
     * <p>
     * Example configuration in YAML:
     * <pre>
     * logging:
     *   accesslog:
     *     trace:
     *       - ".*\/debug\/.*"
     *       - ".*\/monitoring\/.*"
     * </pre>
     */
    List<Pattern> trace = new ArrayList<>();

    /**
     * A list of java regular expressions applied to the request URL for logging at debug level.
     * <p>
     * Requests with URLs matching any of these patterns will be logged at DEBUG level if
     * debug logging is enabled. These patterns should follow Java's regular expression syntax.
     * <p>
     * Example configuration in YAML:
     * <pre>
     * logging:
     *   accesslog:
     *     debug:
     *       - ".*\/admin\/.*"
     *       - ".*\/internal\/.*"
     * </pre>
     */
    List<Pattern> debug = new ArrayList<>();

    /**
     * A list of java regular expressions applied to the request URL for logging at info level.
     * <p>
     * Requests with URLs matching any of these patterns will be logged at INFO level.
     * These patterns should follow Java's regular expression syntax.
     * <p>
     * Example configuration in YAML:
     * <pre>
     * logging:
     *   accesslog:
     *     info:
     *       - ".*\/api\/.*"
     *       - ".*\/public\/.*"
     * </pre>
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

    /**
     * Logs a request with the appropriate log level based on the URI pattern.
     * <p>
     * This method determines the appropriate log level for the given URI by checking it
     * against the configured patterns. It then logs the request details (method, status code, URI)
     * at that level. If no patterns match, the request is not logged.
     * <p>
     * The log format is: {@code METHOD STATUS_CODE URI}
     * <p>
     * Example log output: {@code GET 200 /api/data}
     *
     * @param method the HTTP method (GET, POST, etc.)
     * @param statusCode the HTTP status code (200, 404, etc.)
     * @param uri the request URI
     */
    public void log(String method, int statusCode, String uri) {
        Level level = getLogLevel(uri);
        level.log("{} {} {} ", method, statusCode, uri);
    }

    /**
     * Determines the appropriate log level for a given URI.
     * <p>
     * This method checks the URI against the configured patterns for each log level
     * (info, debug, trace) and returns the highest applicable level. If no patterns match
     * or if logging at the matched level is disabled, it returns Level.OFF.
     * <p>
     * The level is determined in the following order of precedence:
     * <ol>
     *   <li>INFO - if info patterns match and info logging is enabled</li>
     *   <li>DEBUG - if debug patterns match and debug logging is enabled</li>
     *   <li>TRACE - if trace patterns match and trace logging is enabled</li>
     *   <li>OFF - if no patterns match or logging at the matched level is disabled</li>
     * </ol>
     *
     * @param uri the request URI to check
     * @return the appropriate log level for the URI
     */
    Level getLogLevel(String uri) {
        if (log.isInfoEnabled() && matches(uri, info)) return Level.INFO;
        if (log.isDebugEnabled() && matches(uri, debug)) return Level.DEBUG;
        if (log.isTraceEnabled() && matches(uri, trace)) return Level.TRACE;

        return Level.OFF;
    }

    /**
     * Determines if this request should be logged based on the URI.
     * <p>
     * This method checks if the given URI matches any of the configured patterns
     * for info, debug, or trace level logging. If it matches any pattern,
     * the request should be logged (although whether it is actually logged
     * depends on the enabled log levels).
     * <p>
     * This is a quick check used by the filter to determine if a request
     * needs detailed processing for logging.
     *
     * @param uri the request URI to check
     * @return true if the request should be logged (matches any pattern), false otherwise
     */
    public boolean shouldLog(java.net.URI uri) {
        if (uri == null) return false;

        String uriString = uri.toString();
        return matches(uriString, info) || matches(uriString, debug) || matches(uriString, trace);
    }

    /**
     * Checks if a URL matches any of the given patterns.
     * <p>
     * This method tests the provided URL against each pattern in the list.
     * If any pattern matches, the method returns true. If the pattern list
     * is null or empty, it returns false.
     * <p>
     * The comparison is done using {@link java.util.regex.Pattern#matcher(CharSequence).matches()},
     * which checks if the entire URL matches the pattern.
     *
     * @param url the URL to check
     * @param patterns the list of regex patterns to match against
     * @return true if the URL matches any pattern, false otherwise
     */
    private boolean matches(String url, List<Pattern> patterns) {
        return patterns != null
                && !patterns.isEmpty()
                && patterns.stream().anyMatch(pattern -> pattern.matcher(url).matches());
    }
}
