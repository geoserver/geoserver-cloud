/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.app;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.geoserver.catalog.CatalogFacade;
import org.geoserver.cloud.autoconfigure.catalog.DataDirectoryAutoConfiguration;
import org.geoserver.cloud.catalog.test.WebTestClientSupportConfiguration;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.config.GeoServerConfigPersister;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration tests for the application using a {@link DataDirectoryAutoConfiguration} backed
 * catalog
 */
@SpringBootTest(
    classes = {CatalogServiceApplication.class, WebTestClientSupportConfiguration.class},
    properties = {"geoserver.backend.data-directory.enabled=true"}
)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@AutoConfigureWebTestClient(timeout = "360000")
public class CatalogServiceDataDirectoryTest extends CatalogServiceIntegrationTest {

    public @Before void before() {
        CatalogFacade facade = rawCatalogFacade;
        assertThat(
                "check config, a data directory setup uses org.geoserver.catalog.plugin.DefaultCatalogFacade",
                facade,
                instanceOf(org.geoserver.catalog.plugin.DefaultCatalogFacade.class));

        assertThat(
                "check config, a data directory setup uses "
                        + org.geoserver.catalog.plugin.DefaultGeoServerFacade.class.getName(),
                geoServerFacadeImpl,
                instanceOf(org.geoserver.catalog.plugin.DefaultGeoServerFacade.class));
        assertSame(geoServerFacadeImpl, geoServer.getFacade());

        assertThat(
                "check config, a data directory setup uses DefaultGeoServerLoader",
                geoServerLoader,
                instanceOf(DefaultGeoServerLoader.class));
        assertTrue(
                "check config, a data directory setup adds a GeoServerConfigPersister listener",
                catalog.getListeners()
                        .stream()
                        .anyMatch(l -> l instanceof GeoServerConfigPersister));
    }
}
