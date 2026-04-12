/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.catalog.backend.datadir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Path;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.plugin.CatalogPlugin;
import org.geoserver.catalog.plugin.locking.LockProviderGeoServerConfigurationLock;
import org.geoserver.catalog.plugin.locking.LockingCatalog;
import org.geoserver.catalog.plugin.locking.LockingGeoServer;
import org.geoserver.cloud.config.catalog.backend.datadirectory.CloudDataDirectoryGeoServerLoader;
import org.geoserver.cloud.config.catalog.backend.datadirectory.DataDirectoryBackendConfiguration;
import org.geoserver.cloud.config.catalog.backend.datadirectory.DataDirectoryProperties;
import org.geoserver.cloud.config.catalog.backend.datadirectory.DataDirectoryUpdateSequence;
import org.geoserver.config.plugin.RepositoryGeoServerFacade;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.config.UpdateSequence;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Test {@link DataDirectoryBackendConfiguration} through {@link DataDirectoryAutoConfiguration} when
 * {@code geoserver.backend.data-directory.enabled=true}
 */
@Execution(ExecutionMode.SAME_THREAD)
class DataDirectoryAutoConfigurationTest {

    private ApplicationContextRunner runner;

    private Path datadir;

    @BeforeEach
    void beforeEach(@TempDir Path datadir) {
        GeoServerExtensionsHelper.init(null);
        this.datadir = datadir;
        runner = new ApplicationContextRunner()
                .withAllowBeanDefinitionOverriding(true)
                .withAllowCircularReferences(true)
                .withConfiguration(AutoConfigurations.of(
                        // AutoConfigurations from gs-cloud-catalog-backend-common
                        org.geoserver.cloud.autoconfigure.geotools.GeoToolsHttpClientAutoConfiguration.class,
                        org.geoserver.cloud.autoconfigure.catalog.backend.core.GeoServerBackendAutoConfiguration.class,
                        org.geoserver.cloud.autoconfigure.main.DefaultUpdateSequenceAutoConfiguration.class,
                        org.geoserver.cloud.autoconfigure.main.XstreamServiceLoadersAutoConfiguration.class,
                        org.geoserver.cloud.autoconfigure.catalog.backend.core
                                .RemoteEventResourcePoolCleanupUpAutoConfiguration.class,
                        //
                        org.geoserver.cloud.autoconfigure.main.GeoServerMainSecurityAutoConfiguration.class,
                        org.geoserver.cloud.autoconfigure.metrics.catalog.CatalogMetricsAutoConfiguration.class,
                        // AutoConfigurations from gs-cloud-catalog-backend-datadir
                        org.geoserver.cloud.autoconfigure.catalog.backend.datadir.DataDirectoryAutoConfiguration
                                .class //
                        ))
                //
                .withPropertyValues(
                        "geoserver.backend.dataDirectory.enabled=true", //
                        "geoserver.backend.dataDirectory.location=%s".formatted(datadir.toAbsolutePath()) //
                        );
    }

    @Test
    void testProperties() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(DataDirectoryProperties.class);
            assertThat(context)
                    .getBean(DataDirectoryProperties.class)
                    .hasFieldOrPropertyWithValue("location", datadir.toAbsolutePath());
        });
    }

    @Test
    void testCatalog() {
        runner.run(context -> {
            assertThat(context).getBean("rawCatalog").isInstanceOf(LockingCatalog.class);
        });
    }

    @Test
    void testGeoServer() {
        runner.run(context -> {
            assertThat(context).getBean("geoServer").isInstanceOf(LockingGeoServer.class);
        });
    }

    @Test
    void testCatalogFacadeIsRawCatalogFacade() {
        runner.run(context -> {
            CatalogFacade rawCatalogFacade = context.getBean("catalogFacade", CatalogFacade.class);
            CatalogPlugin rawCatalog = context.getBean("rawCatalog", CatalogPlugin.class);
            assertSame(rawCatalogFacade, rawCatalog.getRawFacade());
        });
    }

    @Test
    void testResourceLoader() {
        runner.run(context -> {
            assertThat(context).getBean("resourceLoader").isInstanceOf(GeoServerResourceLoader.class);
        });
    }

    @Test
    void testGeoserverFacade() {
        runner.run(context -> {
            assertThat(context).getBean("geoserverFacade").isInstanceOf(RepositoryGeoServerFacade.class);
        });
    }

    @Test
    void testGeoserverLoader() {
        runner.run(context -> {
            assertThat(context).getBean("geoServerLoaderImpl").isInstanceOf(CloudDataDirectoryGeoServerLoader.class);
        });
    }

    @Test
    void testResourceStoreImpl() {
        runner.run(context -> {
            assertThat(context).getBean("resourceStoreImpl").isInstanceOf(FileSystemResourceStore.class);
        });
    }

    @Test
    void testUpdateSequence() {
        runner.run(context -> {
            assertThat(context).getBean(UpdateSequence.class).isInstanceOf(DataDirectoryUpdateSequence.class);
        });
    }

    @Test
    void testGeoServerConfigurationLock() {
        runner.run(context -> {
            assertThat(context)
                    .getBean(GeoServerConfigurationLock.class)
                    .isInstanceOf(LockProviderGeoServerConfigurationLock.class);
        });
    }
}
