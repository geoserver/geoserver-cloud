/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.logging.profiles;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;

/**
 * Applies a {@link LoggingProfiles logging profile} to the loggers of this service instance.
 *
 * <p>The levels of the profile apply on top of the levels set at startup by the externalized configuration
 * ({@code logging.level.*}). A profile level also applies to the descendants of its logger: their configured levels are
 * cleared. For example, {@code org.geoserver: trace} applies to {@code org.geoserver.catalog} too, although the
 * configuration sets it to {@code info}. This is how a vanilla GeoServer profile behaves, since it replaces the whole
 * logging configuration.
 *
 * <p>Applying a profile first restores the levels changed by the previous one. With an unknown profile name, only the
 * levels of the configuration apply.
 */
@Slf4j
@RequiredArgsConstructor
public class LoggingProfileApplier {

    private final @NonNull LoggingSystem loggingSystem;

    private final @NonNull LoggingProfiles profiles;

    /** The configured levels changed by the current profile, by logger name. A null level means there was none. */
    private final Map<String, LogLevel> replacedLevels = new HashMap<>();

    /**
     * Applies the profile. A null or empty name is {@link LoggingProfiles#DEFAULT_LOGGING}.
     *
     * @param profileName the name of the profile, as stored in {@code LoggingInfo.level}
     */
    public synchronized void apply(String profileName) {
        String name = orDefault(profileName);
        Optional<Map<String, LogLevel>> levels = profiles.levels(name);
        if (levels.isPresent()) {
            log.info("Applying logging profile {}", name);
        } else {
            log.warn(
                    "Logging profile {} is not defined, applying the configured log levels. Defined profiles: {}",
                    name,
                    profiles.names());
        }
        restoreReplacedLevels();
        levels.ifPresent(this::setLevels);
    }

    private static String orDefault(String profileName) {
        if (profileName == null || profileName.isBlank()) {
            return LoggingProfiles.DEFAULT_LOGGING;
        }
        return profileName;
    }

    private void restoreReplacedLevels() {
        replacedLevels.forEach(loggingSystem::setLogLevel);
        replacedLevels.clear();
    }

    private void setLevels(Map<String, LogLevel> levels) {
        clearDescendantLevels(levels.keySet());
        levels.forEach(this::replaceLevel);
    }

    private void clearDescendantLevels(Set<String> loggerNames) {
        for (LoggerConfiguration logger : loggingSystem.getLoggerConfigurations()) {
            String name = logger.getName();
            boolean hasLevel = logger.getConfiguredLevel() != null;
            if (hasLevel && isDescendantOfAny(name, loggerNames)) {
                replaceLevel(name, null);
            }
        }
    }

    private void replaceLevel(String loggerName, LogLevel level) {
        if (!replacedLevels.containsKey(loggerName)) {
            replacedLevels.put(loggerName, configuredLevel(loggerName));
        }
        loggingSystem.setLogLevel(loggerName, level);
    }

    private LogLevel configuredLevel(String loggerName) {
        LoggerConfiguration logger = loggingSystem.getLoggerConfiguration(loggerName);
        if (logger == null) {
            return null;
        }
        return logger.getConfiguredLevel();
    }

    private static boolean isDescendantOfAny(String loggerName, Set<String> ancestorNames) {
        return ancestorNames.stream().anyMatch(ancestor -> isDescendant(loggerName, ancestor));
    }

    private static boolean isDescendant(String loggerName, String ancestorName) {
        if (loggerName.equals(ancestorName)) {
            return false;
        }
        if (LoggingSystem.ROOT_LOGGER_NAME.equals(ancestorName)) {
            return true;
        }
        return loggerName.startsWith(ancestorName + ".");
    }
}
