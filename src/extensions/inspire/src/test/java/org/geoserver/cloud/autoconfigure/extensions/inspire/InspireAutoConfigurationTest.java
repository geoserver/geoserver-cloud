/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.inspire;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.ResourceStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

public class InspireAutoConfigurationTest {

    @TempDir
    File tempDir;

    protected ApplicationContextRunner runner;

    @BeforeEach
    void setUp() {
        runner = createContextRunner(tempDir);
    }

    protected ApplicationContextRunner createContextRunner(File tempDir) {
        ResourceStore resourceStore = new FileSystemResourceStore(tempDir);
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(resourceStore);
        GeoServerDataDirectory datadir = new GeoServerDataDirectory(resourceLoader);

        return new ApplicationContextRunner()
                .withBean(ResourceStore.class, () -> resourceStore)
                .withBean(GeoServerResourceLoader.class, () -> resourceLoader)
                .withBean("dataDirectory", GeoServerDataDirectory.class, () -> datadir)
                .withBean("geoServer", GeoServer.class, GeoServerImpl::new)
                .withConfiguration(AutoConfigurations.of(InspireAutoConfiguration.class));
    }

    /**
     *
     */
    @Test
    void testDisabledByDefault() {
        runner.run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasBean("inspireExtension")
                    .getBean("inspireExtension", ModuleStatus.class)
                    .hasFieldOrPropertyWithValue("available", true)
                    .hasFieldOrPropertyWithValue("enabled", false);
        });
    }

    @Test
    void testDisabledExplicitly() {
        runner.withPropertyValues("geoserver.extension.inspire.enabled=false").run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasBean("inspireExtension")
                    .getBean("inspireExtension", ModuleStatus.class)
                    .hasFieldOrPropertyWithValue("available", true)
                    .hasFieldOrPropertyWithValue("enabled", false);
        });
    }

    @Test
    void testEnabled() {
        runner.withPropertyValues("geoserver.extension.inspire.enabled=true").run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasBean("inspireExtension")
                    .getBean("inspireExtension", ModuleStatus.class)
                    .hasFieldOrPropertyWithValue("available", true)
                    .hasFieldOrPropertyWithValue("enabled", true);
        });
    }
}
