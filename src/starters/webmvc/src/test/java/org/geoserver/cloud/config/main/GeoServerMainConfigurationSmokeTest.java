/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.main;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.cloud.autoconfigure.servlet.DataDirectoryTempSupport;
import org.geoserver.cloud.test.TestConfiguration;
import org.geoserver.security.SecureCatalogImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Smoke test to load the main context without auto-configuration enabled and without security */
@SpringBootTest(classes = {TestConfiguration.class})
@ActiveProfiles("test")
class GeoServerMainConfigurationSmokeTest extends DataDirectoryTempSupport {

    private @Autowired @Qualifier("rawCatalog") Catalog rawCatalog;
    private @Autowired @Qualifier("secureCatalog") Catalog secureCatalog;

    @Test
    void contextLoads() {
        assertThat(rawCatalog, instanceOf(CatalogImpl.class));
        assertThat(secureCatalog, instanceOf(SecureCatalogImpl.class));
    }
}
