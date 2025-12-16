/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.lang.reflect.Field;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.importer.Importer;
import org.geoserver.importer.rest.ImportController;
import org.geoserver.logging.LoggingUtils;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.web.Category;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Tests for {@link ImporterAutoConfiguration}.
 */
class ImporterAutoConfigurationTest {

    /**
     * Base context runner with minimal configuration.
     */
    private ApplicationContextRunner baseRunner;

    @BeforeEach
    void setUp(@TempDir File tmp) throws Exception {

        // required by Importer's constructor using GeoServerExtensions to acquire
        // GeoServerResourceLoader
        GeoServerResourceLoader loader = new GeoServerResourceLoader(tmp);
        GeoServerExtensionsHelper.setIsSpringContext(false);
        GeoServerExtensionsHelper.singleton("resourceLoader", loader, GeoServerResourceLoader.class);

        Catalog catalog = new CatalogImpl();
        catalog.setResourceLoader(loader);

        GeoServerImpl geoServer = new GeoServerImpl();
        geoServer.setCatalog(catalog);

        // set relinquishLog4jControl = true to avoid erros during Importer.onApplicationEvent calling
        // LoggingUtils.checkBuiltInLoggingConfiguration(loader, "IMPORTER_LOGGING.xml")
        Field field = LoggingUtils.class.getDeclaredField("relinquishLog4jControl");
        field.setAccessible(true);
        field.set(null, Boolean.TRUE);

        baseRunner = new ApplicationContextRunner()
                .withBean("catalog", Catalog.class, () -> catalog)
                .withBean("geoServer", GeoServer.class, () -> geoServer)
                .withBean(GeoServerResourceLoader.class, () -> loader)
                // required by importDataMenuPage
                .withBean("dataCategory", org.geoserver.web.Category.class, () -> mock(Category.class))
                // required by importerConfigPage
                .withBean("settingsCategory", org.geoserver.web.Category.class, () -> mock(Category.class))
                .withConfiguration(AutoConfigurations.of(ImporterAutoConfiguration.class))
                .withBean(ContextLoadedEventSubmitter.class, ContextLoadedEventSubmitter::new);
    }

    /**
     * Bean used to publish a GeoServer {@link ContextLoadedEvent} as if
     * {@link GeoServer#reload()} was called, and avoid the following errors since
     * {@link Importer#onApplicationEvent()} expects it to create the store:
     * {@code Invocation of destroy method failed on bean with name 'importer': java.lang.NullPointerException: Cannot invoke "org.geoserver.importer.ImportStore.destroy()" because "this.contextStore" is null}
     */
    static class ContextLoadedEventSubmitter {

        @EventListener(ContextRefreshedEvent.class)
        void publishContextLoadedEvent(ContextRefreshedEvent event) {
            event.getApplicationContext().publishEvent(new ContextLoadedEvent(event.getApplicationContext()));
        }
    }

    @AfterEach
    void tearDown() {
        GeoServerExtensionsHelper.init(null);
    }

    /**
     * Tests that the auto-configuration is disabled by default.
     */
    @Test
    void testDisabledByDefault() {
        baseRunner.run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .doesNotHaveBean(ImporterAutoConfiguration.class)
                    .doesNotHaveBean(ImporterAutoConfiguration.ImporterRestAutoConfiguration.class)
                    .doesNotHaveBean(ImporterAutoConfiguration.ImporterWebUIAutoConfiguration.class)
                    .doesNotHaveBean("importer");
        });
    }

    /**
     * Tests that enabling the property activates the auto-configuration.
     */
    @Test
    void testConditionalActivation() {
        baseRunner
                .withPropertyValues("geoserver.extension.importer.enabled=true")

                // disable ConditionalOnGeoServerWebUI and ConditionalOnGeoServerREST
                .withClassLoader(new FilteredClassLoader(
                        org.geoserver.web.GeoServerApplication.class,
                        org.geoserver.rest.security.RestConfigXStreamPersister.class))
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(ImporterAutoConfiguration.class)
                            .doesNotHaveBean(ImporterAutoConfiguration.ImporterRestAutoConfiguration.class)
                            .doesNotHaveBean(ImporterAutoConfiguration.ImporterWebUIAutoConfiguration.class)
                            .doesNotHaveBean(Importer.class)
                            .getBean(ImporterConfigProperties.class)
                            .hasFieldOrPropertyWithValue("enabled", true);
                });
    }

    /**
     * Tests that the core configuration is activated when required classes are
     * present and the extension is enabled.
     */
    @Test
    void testCoreConfiguration() {
        baseRunner
                .withPropertyValues("geoserver.extension.importer.enabled=true", "geoserver.service.webui.enabled=true")
                .run(context -> {
                    // Core configuration should be activated
                    assertThat(context)
                            .hasBean("importerInfoDao")
                            .hasBean("importer")
                            .hasBean("ImporterCoreExtension")
                            .hasBean("importerContextCleaner")
                            .hasBean("ImporterCoreExtension");
                });
    }

    /**
     * Tests that the core configuration is not activated when the extension is
     * disabled.
     */
    @Test
    void testCoreConfigurationDisabled() {
        baseRunner
                .withPropertyValues(
                        "geoserver.extension.importer.enabled=false", "geoserver.service.webui.enabled=true")
                .run(context -> {
                    // Core configuration should not be activated
                    assertThat(context)
                            .doesNotHaveBean("importerInfoDao")
                            .doesNotHaveBean("importer")
                            .doesNotHaveBean("ImporterCoreExtension")
                            .doesNotHaveBean("importerContextCleaner");
                });
    }

    /**
     * Tests that the web UI configuration is activated when required classes are
     * present and the extension is enabled in a WebUI environment.
     */
    @Test
    void testWebUIConfiguration() {
        // Set up a simulated WebUI environment
        baseRunner
                .withPropertyValues("geoserver.extension.importer.enabled=true", "geoserver.service.webui.enabled=true")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            // Core configuration should be activated
                            .hasBean("importerInfoDao")
                            .hasBean("importer")
                            .hasBean("ImporterCoreExtension")
                            // WebUI configuration should be activated

                            .hasBean("importerWebExtension")
                            .hasBean("importerWebExtension")
                            .hasBean("importDataMenuPage")
                            // no REST beans
                            .doesNotHaveBean("importerRestExtension");
                });
    }

    /**
     * Tests that the REST configuration is activated when required classes are
     * present and the extension is enabled in a REST environment.
     */
    @Test
    void testRESTConfiguration() {
        // Set up a simulated REST environment
        baseRunner
                .withPropertyValues(
                        "geoserver.extension.importer.enabled=true", "geoserver.service.restconfig.enabled=true")
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed()
                            // core configuration
                            .hasBean("importerInfoDao")
                            .hasBean("importer")
                            .hasBean("ImporterCoreExtension")
                            // REST configuration should be activated
                            .hasBean("importerRestExtension")
                            .hasSingleBean(ImportController.class)
                            // no webui beans
                            .doesNotHaveBean("importerWebExtension");
                });
    }

    /**
     * Tests that the REST configuration is not activated when required classes are
     * missing, even if the extension is enabled.
     */
    @Test
    void testRESTConfigurationMissingClasses() {
        baseRunner
                .withPropertyValues(
                        "geoserver.extension.importer.enabled=true", "geoserver.service.restconfig.enabled=true")
                .withClassLoader(new FilteredClassLoader(org.geoserver.importer.rest.ImportBaseController.class))
                .run(context -> {
                    assertThat(context)
                            .doesNotHaveBean("importerInfoDao")
                            .doesNotHaveBean("importer")
                            .doesNotHaveBean("importerRestExtension");
                });
    }
}
