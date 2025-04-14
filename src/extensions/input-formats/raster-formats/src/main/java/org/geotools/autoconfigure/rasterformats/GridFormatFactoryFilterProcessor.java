/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.autoconfigure.rasterformats;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Sets;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.io.GridFormatFactorySpi;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.util.factory.FactoryCreator;
import org.springframework.beans.factory.InitializingBean;
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
@Slf4j(topic = "org.geotools.autoconfigure.rasterformats")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GridFormatFactoryFilterProcessor implements InitializingBean, ApplicationListener<ContextRefreshedEvent> {

    private final GridFormatFactoryFilterConfigProperties config;

    public GridFormatFactoryFilterProcessor(GridFormatFactoryFilterConfigProperties config) {
        this.config = config;
        // Process immediately in the constructor to ensure it happens before
        // any other beans are initialized, especially CloudGeoServerLoaderProxy
        try {
            // Use a different approach - we cannot reliably deregister formats,
            // so we will create a wrapper class that filters formats dynamically
            installGridFormatFilter();
        } catch (Exception e) {
            log.error("Failed to process GridFormatFactorySpi instances", e);
            log.warn(
                    "Raster format filtering disabled due to error. Formats are configured but not filtered at runtime.");
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
            installGridFormatFilter();
        }
    }

    private void debugConfiguration() {
        Map<String, GridFormatFactorySpi> availableFormats = GridFormatFinder.getAvailableFormats().stream()
                .collect(Collectors.toMap(this::getFormatDisplayName, Function.identity()));
        Map<String, Boolean> configuredFormats = config.getRasterFormats();
        Set<String> displayNames = Sets.union(availableFormats.keySet(), configuredFormats.keySet());

        for (String displayName : displayNames) {
            boolean enabled = config.isFormatEnabled(displayName);
            if (enabled) {
                GridFormatFactorySpi factory =
                        requireNonNull(availableFormats.get(displayName), () -> "Expected enabled format not found: %s"
                                .formatted(displayName));
                String className = factory.getClass().getName();
                log.info("GridFormatFactorySpi factory enabled: {} ({})", displayName, className);
            } else {
                log.info("GridFormatFactorySpi factory disabled: {}", displayName);
                if (availableFormats.containsKey(displayName)) {
                    log.warn(
                            "disabled GridFormatFactorySpi {} should not be available but found {}",
                            displayName,
                            availableFormats.get(displayName).getClass().getName());
                }
            }
        }
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
            log.info("Initializing GridFormatFactorySpi filtering");

            // Now create a custom FactoryCreator that filters the factories
            if (originalRegistry instanceof FactoryCreator fc) {
                FilteringFactoryCreator filteringFC = new FilteringFactoryCreator(fc);

                // Replace the registry with our filtered version
                registryField.set(null, filteringFC);

                log.debug("Successfully installed GridFormatFinder filter");
                // Debug configuration
                debugConfiguration();
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
                    GridFormatFactorySpi formatFactory = (GridFormatFactorySpi) factory;
                    String displayName = getFormatDisplayName(formatFactory);
                    return config.isFormatEnabled(displayName);
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
