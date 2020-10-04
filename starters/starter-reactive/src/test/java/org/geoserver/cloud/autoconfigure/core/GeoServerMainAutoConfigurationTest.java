/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.impl.LocalWorkspaceCatalog;
import org.geoserver.cloud.test.TestConfiguration;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = TestConfiguration.class)
@EnableAutoConfiguration
@RunWith(SpringRunner.class)
@ActiveProfiles("test") // see bootstrap-test.yml
public class GeoServerMainAutoConfigurationTest {

    private @Autowired ApplicationContext context;

    public @Test void testContext() {
        assertFalse(context instanceof WebApplicationContext);
        assertTrue(context instanceof ReactiveWebApplicationContext);
    }

    public @Test void rawCatalog() {
        Catalog catalog = (Catalog) context.getBean("rawCatalog");
        Assume.assumeTrue(catalog instanceof org.geoserver.catalog.plugin.CatalogPlugin);
        assertThat(catalog, instanceOf(org.geoserver.catalog.plugin.CatalogPlugin.class));
        CatalogFacade rawCatalogFacade = context.getBean(CatalogFacade.class);
        assertThat(
                rawCatalogFacade,
                instanceOf(org.geoserver.catalog.plugin.DefaultMemoryCatalogFacade.class));
    }

    public @Test void catalog() {
        Catalog catalog = (Catalog) context.getBean("catalog");
        assertThat(catalog, instanceOf(LocalWorkspaceCatalog.class));
    }
}
