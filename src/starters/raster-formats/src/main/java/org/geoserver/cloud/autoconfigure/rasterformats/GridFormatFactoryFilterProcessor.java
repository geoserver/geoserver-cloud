/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.rasterformats;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.io.GridFormatFactorySpi;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.util.factory.FactoryCreator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.ReflectionUtils;

/**
 * A BeanPostProcessor that handles GridFormatFactorySpi filtering by creating
 * a proxy around the GridFormatFinder class to filter formats on-the-fly.
 *
 * <p>This approach is necessary because GridFormatFinder.getAvailableFormats()
 * calls scanForPlugins() each time, which refreshes the registry and reloads
 * factories from the classpath, making it impossible to permanently deregister
 * formats.
 *
 * <p>Also implements ApplicationListener to ensure the filter is reinstalled
 * after the context is fully initialized, as there may be components that
 * call GridFormatFinder during initialization.
 */
@Slf4j(topic = "org.geoserver.cloud.autoconfigure.rasterformats")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GridFormatFactoryFilterProcessor
        implements BeanPostProcessor, InitializingBean, ApplicationListener<ContextRefreshedEvent> {

    private final GridFormatFactoryFilterConfigProperties config;
    private final Set<String> knownFormats = new HashSet<>();

    public GridFormatFactoryFilterProcessor(GridFormatFactoryFilterConfigProperties config) {
        this.config = config;
        // Process immediately in the constructor to ensure it happens before
        // any other beans are initialized, especially CloudGeoServerLoaderProxy
        if (config.isEnabled()) {
            log.info(
                    "GridFormatFactorySpi configuration loaded with {} entries: {}",
                    config.getRasterFormats().size(),
                    config.getRasterFormats());
            try {
                // Use a different approach - we cannot reliably deregister formats,
                // so we will create a wrapper class that filters formats dynamically
                installGridFormatFilter();

                // Log which formats would be disabled according to configuration
                logDisabledFormats();
            } catch (Exception e) {
                log.error("Failed to process GridFormatFactorySpi instances", e);
                log.warn(
                        "Raster format filtering disabled due to error. Formats are configured but not filtered at runtime.");
            }
        } else {
            log.info("GridFormatFactorySpi filtering is disabled");
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Reinstall filter after properties are set
        if (config.isEnabled()) {
            installGridFormatFilter();
        }
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Reinstall filter after context is refreshed
        if (config.isEnabled()) {
            log.info("Context refreshed, reinstalling GridFormatFinder filter");
            // Debug configuration
            debugConfiguration();

            installGridFormatFilter();
            verifyFilteringWorks();
        }
    }

    /**
     * Debug the configuration to identify issues with configuration property binding
     */
    private void debugConfiguration() {
        log.info(
                "GridFormatFactoryFilterConfigProperties: enabled={}, rasterFormats={}",
                config.isEnabled(),
                config.getRasterFormats());

        // For each format in the configuration, log if it's matching formats correctly
        for (Map.Entry<String, Boolean> entry : config.getRasterFormats().entrySet()) {
            // Try with and without brackets
            String formatName = entry.getKey();
            String formatWithBrackets = formatName.startsWith("[") ? formatName : "[" + formatName + "]";
            String formatWithoutBrackets =
                    formatName.startsWith("[") ? formatName.substring(1, formatName.length() - 1) : formatName;

            log.info("Format config entry: '{}' = {}", formatName, entry.getValue());
            log.info("  - isFormatEnabled('{}') = {}", formatWithBrackets, config.isFormatEnabled(formatWithBrackets));
            log.info(
                    "  - isFormatEnabled('{}') = {}",
                    formatWithoutBrackets,
                    config.isFormatEnabled(formatWithoutBrackets));
        }
    }

    /**
     * Verify that our filtering is actually working
     */
    private void verifyFilteringWorks() {
        try {
            // Force a refresh of the formats
            GridFormatFinder.scanForPlugins();

            // Get the available formats after filtering
            Set<GridFormatFactorySpi> formats = GridFormatFinder.getAvailableFormats();

            // Check which formats should be disabled
            boolean allFiltered = true;
            for (GridFormatFactorySpi factory : formats) {
                String displayName = getFormatDisplayName(factory);
                boolean enabled = config.isFormatEnabled(displayName);
                if (!enabled) {
                    Object configValue = config.getRasterFormats().get(displayName);
                    if (displayName.startsWith("[") && displayName.endsWith("]")) {
                        configValue = config.getRasterFormats().get(displayName.substring(1, displayName.length() - 1));
                    }
                    if (configValue == null && !displayName.startsWith("[")) {
                        configValue = config.getRasterFormats().get("[" + displayName + "]");
                    }
                    log.warn(
                            "Format still visible despite being disabled: {} ({}) - config entry: {}",
                            displayName,
                            factory.getClass().getName(),
                            configValue);
                    allFiltered = false;
                }
            }

            if (allFiltered) {
                log.info("All disabled formats are being filtered correctly");
            } else {
                log.warn("Some disabled formats are still visible - filter might be bypassed");
                // Try to reinstall the filter more aggressively
                installGridFormatFilter();
            }
        } catch (Exception e) {
            log.error("Error verifying filter effectiveness", e);
        }
    }

    /**
     * Logs which formats would be disabled by the configuration.
     */
    private void logDisabledFormats() {
        // Count the number of disabled formats
        int disabledCount = 0;
        Set<GridFormatFactorySpi> allFactories = GridFormatFinder.getAvailableFormats();

        // Reset the known formats list
        knownFormats.clear();

        for (GridFormatFactorySpi factory : allFactories) {
            String displayName = getFormatDisplayName(factory);
            boolean enabled = config.isFormatEnabled(displayName);

            // Track this format
            knownFormats.add(displayName);

            if (!enabled) {
                disabledCount++;
                log.info(
                        "Format configured as disabled: {} ({})",
                        displayName,
                        factory.getClass().getName());
            } else {
                log.info(
                        "Format configured as enabled: {} ({})",
                        displayName,
                        factory.getClass().getName());
            }
        }

        log.info(
                "GridFormatFactorySpi filtering: {} of {} formats configured as disabled",
                disabledCount,
                allFactories.size());

        // Log any formats in the configuration that were not found
        for (String configFormat : config.getRasterFormats().keySet()) {
            if (!knownFormats.contains(configFormat)) {
                log.warn("Format in configuration not found: {}", configFormat);
            }
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // If this is after all initialization, reinstall our filter
        // This ensures our filter is the last one applied
        if (config.isEnabled() && beanName.contains("GeoServerLoaderProxy")) {
            log.info("Detected GeoServerLoaderProxy initialization, reinstalling format filter");
            installGridFormatFilter();
        }
        return bean;
    }

    /**
     * Gets the display name for a GridFormatFactorySpi.
     *
     * @param factory the factory
     * @return the display name or the class name if no format can be created
     */
    private String getFormatDisplayName(GridFormatFactorySpi factory) {
        try {
            return factory.createFormat().getName();
        } catch (Exception e) {
            log.debug("Error getting format name, using class name instead", e);
            return factory.getClass().getSimpleName();
        }
    }

    @SuppressWarnings("java:S3011")
    static void removeGridFormatFilter() {
        Field registryField = getRegistryField();
        try {
            registryField.set(null, null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        GridFormatFinder.scanForPlugins();
    }

    /**
     * Installs a wrapper for GridFormatFinder that filters formats at runtime.
     *
     * <p>Since GridFormatFinder.getAvailableFormats() calls scanForPlugins() which reloads
     * all factories from the classpath, we need to create a wrapper that filters the
     * formats at runtime instead of trying to deregister them.
     */
    @SuppressWarnings("java:S3011")
    private void installGridFormatFilter() {
        try {
            // First, force a scanForPlugins to ensure the registry is initialized
            GridFormatFinder.scanForPlugins();

            Field registryField = getRegistryField();

            // Get the registry object
            Object originalRegistry = registryField.get(null);

            // Check if our filter is already installed
            if (originalRegistry instanceof FilteringFactoryCreator) {
                log.debug("Filter already installed, no need to reinstall");
                return;
            }

            if (originalRegistry == null) {
                log.warn("Registry field is null in GridFormatFinder class");

                // Try to create and set a new registry
                try {
                    // Create a new FactoryCreator for GridFormatFactorySpi
                    FactoryCreator newRegistry = new FactoryCreator(GridFormatFactorySpi.class);

                    // Set it as the registry
                    registryField.set(null, newRegistry);

                    // Now try again with the new registry
                    originalRegistry = registryField.get(null);
                    log.info("Created and set a new registry");
                } catch (Exception e) {
                    log.warn("Failed to create a new registry", e);
                    log.warn("GridFormat filtering will not be applied");
                    return;
                }

                // If still null, give up
                if (originalRegistry == null) {
                    log.warn(
                            "Registry field is still null after initialization attempt. GridFormat filtering will not be applied");
                    return;
                }
            }

            // Now create a custom FactoryCreator that filters the factories
            if (originalRegistry instanceof FactoryCreator fc) {
                FilteringFactoryCreator filteringFC = new FilteringFactoryCreator(fc);

                // Replace the registry with our filtered version
                registryField.set(null, filteringFC);

                log.info("Successfully installed GridFormatFinder filter");
            } else {
                log.warn(
                        "Registry is not an instance of FactoryCreator: {}. GridFormat filtering will not be applied",
                        originalRegistry.getClass().getName());
            }
        } catch (Exception e) {
            log.error("Failed to install GridFormatFinder filter. GridFormat filtering will not be applied", e);
        }
    }

    private static Field getRegistryField() {
        // Get the registry field from GridFormatFinder
        Field registryField = ReflectionUtils.findField(GridFormatFinder.class, "registry");
        if (registryField == null) {
            throw new IllegalStateException("Failed to find 'registry' field in GridFormatFinder class");
        }
        // Make the field accessible
        ReflectionUtils.makeAccessible(registryField);

        return registryField;
    }

    /**
     * A custom FactoryCreator that wraps another FactoryCreator and filters its factories
     * based on the configuration.
     */
    private class FilteringFactoryCreator extends FactoryCreator {
        private final FactoryCreator delegate;

        public FilteringFactoryCreator(FactoryCreator delegate) {
            // Use a single category type for GridFormatFactorySpi
            super(GridFormatFactorySpi.class);
            this.delegate = delegate;
        }

        @Override
        public <T> java.util.stream.Stream<T> getFactories(Class<T> category, boolean lazyLoading) {
            // For GridFormatFactorySpi, filter the factories
            if (category.equals(GridFormatFactorySpi.class)) {
                return delegate.getFactories(category, lazyLoading).filter(factory -> {
                    if (!(factory instanceof GridFormatFactorySpi)) {
                        return true; // Not a format factory, pass through
                    }
                    GridFormatFactorySpi formatFactory = (GridFormatFactorySpi) factory;
                    String displayName = getFormatDisplayName(formatFactory);
                    boolean enabled = config.isFormatEnabled(displayName);
                    if (!enabled) {
                        log.debug("Filtering out disabled format: {}", displayName);
                    }
                    return enabled;
                });
            }

            // For other types, delegate as normal
            return delegate.getFactories(category, lazyLoading);
        }

        @Override
        public void scanForPlugins() {
            // Let the delegate scan, then our filter is still applied
            delegate.scanForPlugins();
        }

        @Override
        public void deregisterFactory(Object factory) {
            delegate.deregisterFactory(factory);
        }

        @Override
        public void registerFactory(Object factory) {
            // Only register if this factory would pass our filter
            if (factory instanceof GridFormatFactorySpi formatFactory) {
                String displayName = getFormatDisplayName(formatFactory);
                if (config.isFormatEnabled(displayName)) {
                    delegate.registerFactory(factory);
                } else {
                    log.debug("Prevented registration of disabled format: {}", displayName);
                }
            } else {
                delegate.registerFactory(factory);
            }
        }

        @Override
        public String toString() {
            return "FilteringFactoryCreator[delegate=" + delegate + "]";
        }
    }
}
