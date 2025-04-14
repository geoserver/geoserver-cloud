/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.autoconfigure.rasterformats;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * Tests for {@link GridFormatFactoryFilterProcessor}.
 */
class GridFormatFactoryFilterProcessorTest {

    private GridFormatFactoryFilterConfigProperties config;
    private GridFormatFactoryFilterProcessor processor;
    private GenericWebApplicationContext context;

    @BeforeEach
    void setUp() {
        // Create a processor with a config that disables a clearly named fictional format
        config = new GridFormatFactoryFilterConfigProperties();
        config.setEnabled(true);

        Map<String, Boolean> formats = new HashMap<>();
        // Use actual format names that might exist
        formats.put("GeoTIFF", false);
        formats.put("ImageMosaic", false);
        config.setRasterFormats(formats);

        // Create application context for testing ApplicationListener
        context = new GenericWebApplicationContext(new MockServletContext());
    }

    @AfterEach
    void tearDown() {
        // Clean up context if opened
        if (context.isActive()) {
            context.close();
        }
        GridFormatFactoryFilterProcessor.removeGridFormatFilter();
    }

    @Test
    void testFilteringConfigLoaded() {
        // Create the processor - it should install the filter at construction time
        processor = new GridFormatFactoryFilterProcessor(config);

        // Just check the number of formats in the configuration
        int configuredFormatsCount = config.getRasterFormats().size();
        assertThat(configuredFormatsCount).isEqualTo(2);

        // Check that the filter was installed - registry should be a FilteringFactoryCreator
        boolean filteringInstalled = checkFilterIsInstalled();
        assertThat(filteringInstalled).isTrue();

        // Verify the filter stays in place after a second scan
        GridFormatFinder.scanForPlugins();
        filteringInstalled = checkFilterIsInstalled();
        assertThat(filteringInstalled).isTrue();
    }

    @Test
    void testApplicationEvents() throws Exception {
        // Create the processor
        processor = new GridFormatFactoryFilterProcessor(config);

        // Test afterPropertiesSet
        processor.afterPropertiesSet();

        // Check filter is installed
        boolean filteringInstalled = checkFilterIsInstalled();
        assertThat(filteringInstalled).isTrue();

        // Publish context refreshed event
        processor.onApplicationEvent(new ContextRefreshedEvent(context));

        // Filter should still be installed after event
        filteringInstalled = checkFilterIsInstalled();
        assertThat(filteringInstalled).isTrue();
    }

    @Test
    void testVerifyFilteringWorks() {
        // Force a scan and get formats
        GridFormatFinder.scanForPlugins();
        Set<String> formats = getAvailableFormats();

        assertThat(formats).contains("GeoTIFF").contains("ImageMosaic");

        // Create the processor with specific disabling of real formats
        processor = new GridFormatFactoryFilterProcessor(config);

        // If formats exist, application context event should trigger filtering
        processor.onApplicationEvent(new ContextRefreshedEvent(context));

        // Get formats again
        formats = getAvailableFormats();

        assertThat(formats).doesNotContain("GeoTIFF").doesNotContain("ImageMosaic");
    }

    private Set<String> getAvailableFormats() {
        return GridFormatFinder.getAvailableFormats().stream()
                .map(f -> f.createFormat().getName())
                .collect(Collectors.toSet());
    }

    /**
     * Checks if the filter was installed by checking the registry class name
     */
    private boolean checkFilterIsInstalled() {
        try {
            // Check if the registry is our FilteringFactoryCreator
            java.lang.reflect.Field registryField = GridFormatFinder.class.getDeclaredField("registry");
            registryField.setAccessible(true);
            Object registry = registryField.get(null);

            // If the registry is null, the filter couldn't be installed
            if (registry == null) {
                return false;
            }

            // Check the class - it should be FilteringFactoryCreator
            String className = registry.getClass().getSimpleName();
            return className.equals("FilteringFactoryCreator");
        } catch (Exception e) {
            // If there's an exception, the filter couldn't be checked
            return false;
        }
    }
}
