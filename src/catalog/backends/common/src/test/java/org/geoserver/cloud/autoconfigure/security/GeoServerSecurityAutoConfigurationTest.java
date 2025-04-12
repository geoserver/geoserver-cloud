/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.List;
import lombok.SneakyThrows;
import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.config.factory.FilteringXmlBeanDefinitionReader;
import org.geoserver.cloud.security.CloudGeoServerSecurityManager;
import org.geoserver.cloud.security.GeoServerSecurityConfiguration;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.TestResourceAccessManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * {@link EnableAutoConfiguration @EnableAutoConfiguration} tests for {@link
 * GeoServerSecurityAutoConfiguration}
 *
 * @since 1.0
 */
class GeoServerSecurityAutoConfigurationTest {

    @TempDir
    File tempDir;

    private ApplicationContextRunner runner;

    @BeforeEach
    void setUp() {
        runner = createContextRunner(tempDir);
    }

    @SneakyThrows(Exception.class)
    static ApplicationContextRunner createContextRunner(File tempDir) {
        Catalog rawCatalog = mock(Catalog.class);
        ResourceAccessManager resourceAccessManager = new TestResourceAccessManager();
        SecureCatalogImpl secureCatalog = new SecureCatalogImpl(rawCatalog, resourceAccessManager);
        GeoServer geoserver = mock(GeoServer.class);
        ResourceStore resourceStore = new FileSystemResourceStore(tempDir);
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(resourceStore);
        GeoServerDataDirectory datadir = new GeoServerDataDirectory(resourceLoader);
        UpdateSequence updateSequence = mock(UpdateSequence.class);

        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(GeoServerSecurityAutoConfiguration.class))
                .withBean("extensions", GeoServerExtensions.class)
                .withBean(ResourceStore.class, () -> resourceStore)
                .withBean(GeoServerResourceLoader.class, () -> resourceLoader)
                .withBean("dataDirectory", GeoServerDataDirectory.class, () -> datadir)
                .withBean("rawCatalog", Catalog.class, () -> rawCatalog)
                .withBean("catalog", Catalog.class, () -> secureCatalog)
                .withBean("secureCatalog", Catalog.class, () -> secureCatalog)
                .withBean("geoServer", GeoServer.class, () -> geoserver)
                .withBean("updateSequence", UpdateSequence.class, () -> updateSequence)
                .withPropertyValues("logging.level.org.geoserver.platform: off");
    }

    @Test
    void disabled() {
        runner.withPropertyValues("geoserver.security.enabled: false") //
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(CloudGeoServerSecurityManager.class);
                });
    }

    @Test
    void enabledByDefault() {
        testEnabled(runner);
    }

    @Test
    void enabledExplicitly() {
        testEnabled(runner.withPropertyValues("geoserver.security.enabled: true"));
    }

    private void testEnabled(ApplicationContextRunner runner) {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("authenticationManager");
            assertThat(context.getBean("authenticationManager")).isInstanceOf(CloudGeoServerSecurityManager.class);

            // gs-main's applicationSecurityContext.xml contributions
            SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
            new FilteringXmlBeanDefinitionReader(registry)
                    .loadBeanDefinitions(GeoServerSecurityConfiguration.APPLICATION_SECURITY_CONTEXT_FILTER);
            List<String> names = List.of(registry.getBeanDefinitionNames());
            assertThat(names).isNotEmpty();
            for (String name : names) {
                assertThat(context).hasBean(name);
            }
        });
    }
}
