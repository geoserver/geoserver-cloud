/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.logging.profiles;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;

/**
 * The logging profiles available to the services: {@link #DEFAULT_LOGGING}, and the ones defined in the
 * {@link LoggingProfilesProperties externalized configuration}.
 */
public class LoggingProfiles {

    /**
     * The profile applied when none is selected, named after the default profile of vanilla GeoServer. It sets no
     * level: the levels of the externalized configuration ({@code logging.level.*}) apply. It is always defined, to let
     * administrators go back to those levels.
     */
    public static final String DEFAULT_LOGGING = "DEFAULT_LOGGING";

    private final Map<String, Map<String, LogLevel>> profiles = new LinkedHashMap<>();

    public LoggingProfiles(@NonNull LoggingProfilesProperties config) {
        profiles.put(DEFAULT_LOGGING, Map.of());
        config.getProfiles().forEach(this::add);
    }

    /**
     * @return the profile names, in the order listed by the web UI: {@link #DEFAULT_LOGGING} first, then the profiles
     *     in the order of the configuration
     */
    public List<String> names() {
        return List.copyOf(profiles.keySet());
    }

    /** @return the logger levels of the profile, by logger name, or empty if there is no profile with that name */
    public Optional<Map<String, LogLevel>> levels(String profileName) {
        if (profileName == null) {
            return Optional.empty();
        }
        Map<String, LogLevel> levels = profiles.get(profileName);
        return Optional.ofNullable(levels).map(Map::copyOf);
    }

    private void add(String profileName, Map<String, LogLevel> levels) {
        Map<String, LogLevel> profile = new HashMap<>();
        levels.forEach((loggerName, level) -> profile.put(normalize(loggerName), level));
        profiles.put(profileName, profile);
    }

    /**
     * The configuration may name the root logger in lower case, as {@code logging.level.root} does. The
     * {@link LoggingSystem} reports it as {@link LoggingSystem#ROOT_LOGGER_NAME}.
     */
    private static String normalize(String loggerName) {
        if (LoggingSystem.ROOT_LOGGER_NAME.equalsIgnoreCase(loggerName)) {
            return LoggingSystem.ROOT_LOGGER_NAME;
        }
        return loggerName;
    }
}
