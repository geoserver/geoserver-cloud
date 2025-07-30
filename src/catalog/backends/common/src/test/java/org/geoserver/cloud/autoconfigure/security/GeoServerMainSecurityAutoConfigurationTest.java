/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import lombok.SneakyThrows;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.configuration.core.main.GeoServerMainSecurityConfiguration;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.security.BruteForceListener;
import org.geoserver.security.CloudGeoServerSecurityManager;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.filter.GeoServerRoleProvider;
import org.geoserver.security.filter.GeoServerSecurityInterceptorProvider;
import org.geoserver.security.filter.GeoServerX509CertificateAuthenticationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * {@link EnableAutoConfiguration @EnableAutoConfiguration} tests for {@link
 * GeoServerMainSecurityConfiguration}
 *
 * @since 1.0
 */
class GeoServerMainSecurityAutoConfigurationTest {

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
                .withConfiguration(AutoConfigurations.of(GeoServerMainSecurityConfiguration.class))
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
    void testOverrides() {
        runner.run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasBean("authenticationManager")
                    .getBean("authenticationManager")
                    .isInstanceOf(CloudGeoServerSecurityManager.class);
        });
    }

    /**
     * Check some beans contributed by {@link GeoServerMainSecurityConfiguration}
     */
    @Test
    void testTranspiledConfiguration() {
        runner.run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(GeoServerRoleProvider.class)
                    .hasSingleBean(GeoServerX509CertificateAuthenticationProvider.class)
                    .hasSingleBean(GeoServerSecurityInterceptorProvider.class)
                    .hasSingleBean(BruteForceListener.class);
        });
    }
}
