/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.app;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import org.geoserver.catalog.CatalogFacade;
import org.geoserver.cloud.autoconfigure.catalog.JDBCConfigAutoConfiguration;
import org.geoserver.cloud.catalog.test.WebTestClientSupportConfiguration;
import org.geoserver.cloud.config.jdbcconfig.CloudJdbcGeoserverFacade;
import org.geoserver.config.GeoServerConfigPersister;
import org.geoserver.jdbcconfig.JDBCGeoServerLoader;
import org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration tests for the application using a {@link JDBCConfigAutoConfiguration} backed catalog
 */
@SpringBootTest(
    classes = {CatalogServiceApplication.class, WebTestClientSupportConfiguration.class},
    properties = {
        "geoserver.backend.data-directory.enabled=false",
        "geoserver.backend.jdbcconfig.enabled=true"
    }
)
@RunWith(SpringRunner.class)
@ActiveProfiles("test") // see bootstrap-test.yml
@AutoConfigureWebTestClient(timeout = "360000")
public class CatalogServiceJdbcConfigTest extends CatalogServiceIntegrationTest {

    public @Before void before() {
        CatalogFacade facade = rawCatalogFacade;
        assertThat(
                "check config, jdbcconfig setup uses " + JDBCCatalogFacade.class.getSimpleName(),
                facade,
                instanceOf(JDBCCatalogFacade.class));

        assertThat(
                "check config, jdbcconfig setup uses "
                        + CloudJdbcGeoserverFacade.class.getSimpleName(),
                geoServerFacadeImpl,
                instanceOf(CloudJdbcGeoserverFacade.class));
        assertSame(geoServerFacadeImpl, geoServer.getFacade());

        assertThat(
                "check config, jdbcconfig  setup uses JDBCGeoServerLoader",
                geoServerLoader,
                instanceOf(JDBCGeoServerLoader.class));
        assertFalse(
                "check config, jdbcconfig setup does not add a GeoServerConfigPersister listener",
                catalog.getListeners()
                        .stream()
                        .anyMatch(l -> l instanceof GeoServerConfigPersister));
    }
}
