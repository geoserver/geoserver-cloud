/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.configuration.extension.ogcapi.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.reflect.Field;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.logging.LoggingUtils;
import org.geoserver.ows.ClasspathPublisher;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class OgcApiCoreWebConfigurationTest {

    private WebApplicationContextRunner runner;

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

        // set relinquishLog4jControl = true to avoid erros during
        // Importer.onApplicationEvent calling
        // LoggingUtils.checkBuiltInLoggingConfiguration(loader, "IMPORTER_LOGGING.xml")
        Field field = LoggingUtils.class.getDeclaredField("relinquishLog4jControl");
        field.setAccessible(true);
        field.set(null, Boolean.TRUE);

        runner = new WebApplicationContextRunner()
                .withBean("catalog", Catalog.class, () -> catalog)
                .withBean("geoServer", GeoServer.class, () -> geoServer)
                .withBean(GeoServerResourceLoader.class, () -> loader)
                .withBean("classpathPublisher", ClasspathPublisher.class, ClasspathPublisher::new)
                .withConfiguration(
                        UserConfigurations.of(OgcApiCoreConfiguration.class, OgcApiCoreWebConfiguration.class));
    }

    @Test
    void testExpectedComponents() {
        runner.run(context ->
                assertThat(context).hasNotFailed().hasBean("ogcapiLayerConfig").hasBean("ogcapiLinksSettings"));
    }
}
