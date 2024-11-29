package org.geoserver.cloud.autoconfigure.gwc.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.ConditionalOnPgconfigBackendEnabled;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.PgconfigBackendAutoConfiguration;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.PgconfigDataSourceAutoConfiguration;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.PgconfigMigrationAutoConfiguration;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.PgconfigTransactionManagerAutoConfiguration;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheEnabled;
import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheContextRunner;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.gwc.backend.pgconfig.PgconfigTileLayerCatalog;
import org.geoserver.cloud.gwc.repository.CachingTileLayerCatalog;
import org.geoserver.cloud.gwc.repository.CloudCatalogConfiguration;
import org.geoserver.cloud.gwc.repository.CloudDefaultStorageFinder;
import org.geoserver.cloud.gwc.repository.CloudGwcXmlConfiguration;
import org.geoserver.cloud.gwc.repository.CloudXMLResourceProvider;
import org.geoserver.cloud.gwc.repository.GeoServerTileLayerConfiguration;
import org.geoserver.cloud.gwc.repository.ResourceStoreTileLayerCatalog;
import org.geoserver.gwc.config.GWCInitializer;
import org.geowebcache.config.ConfigurationResourceProvider;
import org.geowebcache.config.TileLayerConfiguration;
import org.geowebcache.config.XMLConfiguration;
import org.geowebcache.storage.DefaultStorageFinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** GWC integration test for {@link PgconfigTileLayerCatalogAutoConfiguration} */
@Testcontainers(disabledWithoutDocker = true)
class PgconfigTileLayerCatalogAutoConfigurationTest {

    @Container
    static PgConfigTestContainer<?> container = new PgConfigTestContainer<>();

    @TempDir
    File cacheDir;

    private WebApplicationContextRunner runner;

    @BeforeEach
    void setUp() {
        runner = GeoWebCacheContextRunner.newMinimalGeoWebCacheContextRunner(cacheDir)
                .withConfiguration(AutoConfigurations.of(
                        PgconfigTileLayerCatalogAutoConfiguration.class,
                        PgconfigBackendAutoConfiguration.class,
                        PgconfigDataSourceAutoConfiguration.class,
                        PgconfigTransactionManagerAutoConfiguration.class,
                        PgconfigMigrationAutoConfiguration.class));
        runner = container.setUp().withJdbcUrlConfig(runner);
    }

    @AfterEach
    void tearDown() {
        container.tearDown();
    }

    /**
     * {@link TileLayerConfiguration} implementation should be {@link PgconfigTileLayerCatalog}, and
     * none of the beans from {@link DefaultTileLayerCatalogAutoConfiguration} should be present.
     *
     * <p>"pgconfig" is enabled already by {@link PgConfigTestContainer#withJdbcUrlConfig()}
     */
    @Test
    void testPgconfigTileLayerCatalogReplacesDefaultTileLayerCatalogAutoConfiguration() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(GWCInitializer.class).hasSingleBean(PgconfigGwcInitializer.class);

            assertThat(context)
                    .hasNotFailed()
                    .hasBean("gwcCatalogConfiguration")
                    .getBean("gwcCatalogConfiguration", TileLayerConfiguration.class)
                    .isInstanceOf(GeoServerTileLayerConfiguration.class);

            assertThat(context.getBean("gwcCatalogConfiguration", GeoServerTileLayerConfiguration.class)
                            .getSubject())
                    .isInstanceOf(PgconfigTileLayerCatalog.class);

            assertThat(context)
                    .getBean("gwcXmlConfig", XMLConfiguration.class)
                    .isInstanceOf(CloudGwcXmlConfiguration.class);

            assertThat(context)
                    .getBean("gwcXmlConfigResourceProvider", ConfigurationResourceProvider.class)
                    .isInstanceOf(CloudXMLResourceProvider.class);

            assertThat(context)
                    .getBean("gwcDefaultStorageFinder", DefaultStorageFinder.class)
                    .isInstanceOf(CloudDefaultStorageFinder.class);

            assertDefaultTileLayerCatalogConfigurationAbsent(context);
        });
    }

    /**
     * {@link PgconfigTileLayerCatalogAutoConfiguration}'s
     * {@code @ConditionalOnClass(PgconfigTileLayerCatalog.class)}
     */
    @Test
    void conditionalOnClass_PgconfigTileLayerCatalog() {
        runner.withClassLoader(new FilteredClassLoader(PgconfigTileLayerCatalog.class))
                .run(context -> {
                    assertThat(context).hasNotFailed().doesNotHaveBean(PgconfigTileLayerCatalog.class);
                    assertDefaultTileLayerCatalogConfigurationPresent(context);
                });
    }

    /**
     * {@link PgconfigTileLayerCatalogAutoConfiguration}'s {@link
     * ConditionalOnGeoWebCacheEnabled @ConditionalOnGeoWebCacheEnabled} shall not have the pqsl nor
     * the default {@link TileLayerConfiguration}
     */
    @Test
    void conditionalOnGeoWebCacheEnabled() {
        runner.withPropertyValues("gwc.enabled: false").run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(PgconfigTileLayerCatalog.class);
            assertDefaultTileLayerCatalogConfigurationAbsent(context);
        });
    }

    /**
     * {@link PgconfigTileLayerCatalogAutoConfiguration}'s {@link
     * ConditionalOnPgconfigBackendEnabled @ConditionalOnPgconfigBackendEnabled} shall step back and
     * leave {@link DefaultTileLayerCatalogAutoConfiguration} be
     */
    @Test
    void conditionalOnPgconfigBackendEnabled() {
        runner.withPropertyValues("geoserver.backend.pgconfig.enabled: false").run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(PgconfigTileLayerCatalog.class);
            assertDefaultTileLayerCatalogConfigurationPresent(context);
        });
    }

    private void assertDefaultTileLayerCatalogConfigurationAbsent(AssertableWebApplicationContext context) {

        assertThat(context)
                .doesNotHaveBean(CloudCatalogConfiguration.class)
                .doesNotHaveBean(ResourceStoreTileLayerCatalog.class)
                .doesNotHaveBean(CachingTileLayerCatalog.class);
    }

    private void assertDefaultTileLayerCatalogConfigurationPresent(AssertableWebApplicationContext context) {

        assertThat(context)
                .hasSingleBean(ResourceStoreTileLayerCatalog.class)
                .hasSingleBean(CachingTileLayerCatalog.class)
                .hasBean("gwcCatalogConfiguration")
                .getBean("gwcCatalogConfiguration", TileLayerConfiguration.class)
                .isInstanceOf(GeoServerTileLayerConfiguration.class);

        assertThat(context.getBean("gwcCatalogConfiguration", GeoServerTileLayerConfiguration.class)
                        .getSubject())
                .isInstanceOf(CloudCatalogConfiguration.class);
    }
}
