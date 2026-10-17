/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.web.ui;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.cloud.logging.profiles.LoggingProfiles;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.admin.GlobalSettingsPage;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.LogLevel;
import org.springframework.core.env.Environment;

/**
 * Lists the logger levels of a {@link LoggingProfiles logging profile}, next to the "Logging Profile" choice of the
 * {@link GlobalSettingsPage}.
 *
 * <p>{@link LoggingProfiles#DEFAULT_LOGGING} sets no level, and keeps the levels of the configuration
 * ({@code logging.level.*}). The panel lists those levels instead. It also lists them for an unknown profile, because
 * the services then apply the levels of the configuration only.
 */
public class LoggingProfileLevelsPanel extends GenericPanel<String> {

    /** The root logger first, then the other loggers by name */
    private static final Comparator<LoggerLevel> LOGGER_ORDER =
            Comparator.comparing(LoggerLevel::isNotRoot).thenComparing(LoggerLevel::logger);

    /** @param profileName the name of the profile, {@link LoggingProfiles#DEFAULT_LOGGING} if null */
    public LoggingProfileLevelsPanel(String id, IModel<String> profileName) {
        super(id, profileName);
        setOutputMarkupId(true);
        add(new Label("description", this::description));
        add(new LevelsView("levels", LoadableDetachableModel.of(this::levels)));
    }

    private String description() {
        Optional<Map<String, LogLevel>> profileLevels = profileLevels();
        if (profileLevels.isEmpty()) {
            return getString("unknownProfile");
        }
        if (profileLevels.get().isEmpty()) {
            return getString("configuredLevels");
        }
        return getString("profileLevels");
    }

    private List<LoggerLevel> levels() {
        Map<String, LogLevel> profileLevels = profileLevels().orElse(Map.of());
        List<LoggerLevel> levels = profileLevels.isEmpty() ? configuredLevels() : toLoggerLevels(profileLevels);
        return levels.stream().sorted(LOGGER_ORDER).toList();
    }

    /** @return the levels set by the selected profile, or empty if there is no profile with that name */
    private Optional<Map<String, LogLevel>> profileLevels() {
        LoggingProfiles profiles = GeoServerApplication.get().getBeanOfType(LoggingProfiles.class);
        return profiles.levels(profileName());
    }

    private String profileName() {
        String name = getModelObject();
        if (name == null) {
            return LoggingProfiles.DEFAULT_LOGGING;
        }
        return name;
    }

    private static List<LoggerLevel> toLoggerLevels(Map<String, LogLevel> levels) {
        return levels.entrySet().stream()
                .map(level -> LoggerLevel.of(level.getKey(), level.getValue().name()))
                .toList();
    }

    /** @return the levels of {@code logging.level.*}, in the configuration of this service */
    private static List<LoggerLevel> configuredLevels() {
        Environment environment =
                GeoServerApplication.get().getApplicationContext().getEnvironment();
        Binder binder = Binder.get(environment);
        Map<String, String> levels = binder.bind("logging.level", Bindable.mapOf(String.class, String.class))
                .orElse(Map.of());
        return levels.entrySet().stream()
                .map(level -> LoggerLevel.of(level.getKey(), level.getValue()))
                .toList();
    }

    /** A logger and its level, in upper case */
    public record LoggerLevel(String logger, String level) implements Serializable {

        private static final String ROOT = "root";

        /** Names the root logger in lower case, as the configuration does. The profiles name it {@code ROOT}. */
        static LoggerLevel of(String logger, String level) {
            String name = ROOT.equalsIgnoreCase(logger) ? ROOT : logger;
            return new LoggerLevel(name, level.toUpperCase(Locale.ROOT));
        }

        boolean isNotRoot() {
            return !ROOT.equals(logger);
        }
    }

    private static class LevelsView extends ListView<LoggerLevel> {

        LevelsView(String id, IModel<List<LoggerLevel>> levels) {
            super(id, levels);
        }

        @Override
        protected void populateItem(ListItem<LoggerLevel> item) {
            LoggerLevel level = item.getModelObject();
            item.add(new Label("logger", level.logger()));
            item.add(new Label("level", level.level()));
        }
    }
}
