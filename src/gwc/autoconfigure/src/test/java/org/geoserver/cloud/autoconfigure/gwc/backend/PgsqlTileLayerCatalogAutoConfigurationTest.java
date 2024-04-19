package org.geoserver.cloud.autoconfigure.gwc.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.ConditionalOnPgsqlBackendEnabled;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.PgsqlBackendAutoConfiguration;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.PgsqlDataSourceAutoConfiguration;
import org.geoserver.cloud.autoconfigure.catalog.backend.pgconfig.PgsqlMigrationAutoConfiguration;
import org.geoserver.cloud.autoconfigure.gwc.ConditionalOnGeoWebCacheEnabled;
import org.geoserver.cloud.autoconfigure.gwc.GeoWebCacheContextRunner;
import org.geoserver.cloud.backend.pgconfig.support.PgConfigTestContainer;
import org.geoserver.cloud.gwc.backend.pgconfig.PgsqlTileLayerCatalog;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;

/** GWC integration test for {@link PgsqlTileLayerCatalogAutoConfiguration} */
@Testcontainers(disabledWithoutDocker = true)
class PgsqlTileLayerCatalogAutoConfigurationTest {

    @Container static PgConfigTestContainer<?> container = new PgConfigTestContainer<>();

    @TempDir File cacheDir;
    private WebApplicationContextRunner runner;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {}

    @AfterAll
    static void tearDownAfterClass() throws Exception {}

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception {
        runner =
                GeoWebCacheContextRunner.newMinimalGeoWebCacheContextRunner(cacheDir)
                        .withConfiguration(
                                AutoConfigurations.of(
                                        PgsqlTileLayerCatalogAutoConfiguration.class,
                                        PgsqlBackendAutoConfiguration.class,
                                        PgsqlDataSourceAutoConfiguration.class,
                                        PgsqlMigrationAutoConfiguration.class));
        runner = container.setUp().withJdbcUrlConfig(runner);
    }

    @AfterEach
    void tearDown() throws Exception {
        container.tearDown();
    }

    /**
     * {@link TileLayerConfiguration} implementation should be {@link PgsqlTileLayerCatalog}, and
     * none of the beans from {@link DefaultTileLayerCatalogAutoConfiguration} should be present.
     *
     * <p>"pgconfig" is enabled already by {@link PgConfigTestContainer#withJdbcUrlConfig()}
     */
    @Test
    void testPgsqlTileLayerCatalogReplacesDefaultTileLayerCatalogAutoConfiguration() {
        runner.run(
                context -> {
                    assertThat(context)
                            .doesNotHaveBean(GWCInitializer.class)
                            .hasSingleBean(PgsqlGwcInitializer.class);

                    assertThat(context)
                            .hasNotFailed()
                            .hasBean("gwcCatalogConfiguration")
                            .getBean("gwcCatalogConfiguration", TileLayerConfiguration.class)
                            .isInstanceOf(GeoServerTileLayerConfiguration.class);

                    assertThat(
                                    context.getBean(
                                                    "gwcCatalogConfiguration",
                                                    GeoServerTileLayerConfiguration.class)
                                            .getSubject())
                            .isInstanceOf(PgsqlTileLayerCatalog.class);

                    assertThat(context)
                            .getBean("gwcXmlConfig", XMLConfiguration.class)
                            .isInstanceOf(CloudGwcXmlConfiguration.class);

                    assertThat(context)
                            .getBean(
                                    "gwcXmlConfigResourceProvider",
                                    ConfigurationResourceProvider.class)
                            .isInstanceOf(CloudXMLResourceProvider.class);

                    assertThat(context)
                            .getBean("gwcDefaultStorageFinder", DefaultStorageFinder.class)
                            .isInstanceOf(CloudDefaultStorageFinder.class);

                    assertDefaultTileLayerCatalogConfigurationAbsent(context);
                });
    }

    /**
     * {@link PgsqlTileLayerCatalogAutoConfiguration}'s
     * {@code @ConditionalOnClass(PgsqlTileLayerCatalog.class)}
     */
    @Test
    void conditionalOnClass_PgsqlTileLayerCatalog() {
        runner.withClassLoader(new FilteredClassLoader(PgsqlTileLayerCatalog.class))
                .run(
                        context -> {
                            assertThat(context)
                                    .hasNotFailed()
                                    .doesNotHaveBean(PgsqlTileLayerCatalog.class);
                            assertDefaultTileLayerCatalogConfigurationPresent(context);
                        });
    }

    /**
     * {@link PgsqlTileLayerCatalogAutoConfiguration}'s {@link
     * ConditionalOnGeoWebCacheEnabled @ConditionalOnGeoWebCacheEnabled} shall not have the pqsl nor
     * the default {@link TileLayerConfiguration}
     */
    @Test
    void conditionalOnGeoWebCacheEnabled() {
        runner.withPropertyValues("gwc.enabled: false")
                .run(
                        context -> {
                            assertThat(context)
                                    .hasNotFailed()
                                    .doesNotHaveBean(PgsqlTileLayerCatalog.class);
                            assertDefaultTileLayerCatalogConfigurationAbsent(context);
                        });
    }

    /**
     * {@link PgsqlTileLayerCatalogAutoConfiguration}'s {@link
     * ConditionalOnPgsqlBackendEnabled @ConditionalOnPgsqlBackendEnabled} shall step back and leave
     * {@link DefaultTileLayerCatalogAutoConfiguration} be
     */
    @Test
    void conditionalOnPgsqlBackendEnabled() {
        runner.withPropertyValues("geoserver.backend.pgconfig.enabled: false")
                .run(
                        context -> {
                            assertThat(context)
                                    .hasNotFailed()
                                    .doesNotHaveBean(PgsqlTileLayerCatalog.class);
                            assertDefaultTileLayerCatalogConfigurationPresent(context);
                        });
    }

    private void assertDefaultTileLayerCatalogConfigurationAbsent(
            AssertableWebApplicationContext context) {

        assertThat(context)
                .doesNotHaveBean(CloudCatalogConfiguration.class)
                .doesNotHaveBean(ResourceStoreTileLayerCatalog.class)
                .doesNotHaveBean(CachingTileLayerCatalog.class);
    }

    private void assertDefaultTileLayerCatalogConfigurationPresent(
            AssertableWebApplicationContext context) {

        assertThat(context)
                .hasSingleBean(ResourceStoreTileLayerCatalog.class)
                .hasSingleBean(CachingTileLayerCatalog.class)
                .hasBean("gwcCatalogConfiguration")
                .getBean("gwcCatalogConfiguration", TileLayerConfiguration.class)
                .isInstanceOf(GeoServerTileLayerConfiguration.class);

        assertThat(
                        context.getBean(
                                        "gwcCatalogConfiguration",
                                        GeoServerTileLayerConfiguration.class)
                                .getSubject())
                .isInstanceOf(CloudCatalogConfiguration.class);
    }
}
