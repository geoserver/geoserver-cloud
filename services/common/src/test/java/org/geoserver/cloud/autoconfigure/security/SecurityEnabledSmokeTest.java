/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.cloud.test.TestConfiguration;
import org.geoserver.security.SecureCatalogImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Smoke test to load the main context with auto-configuration (both main and security enabled by
 * default)
 */
@SpringBootTest(classes = TestConfiguration.class)
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class})
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class SecurityEnabledSmokeTest {

    private @Autowired @Qualifier("rawCatalog") Catalog rawCatalog;
    private @Autowired @Qualifier("secureCatalog") Catalog secureCatalog;

    private @Autowired ApplicationContext context;

    @Test
    public void contextLoads() {
        assertThat(rawCatalog, instanceOf(CatalogImpl.class));
        assertThat(secureCatalog, instanceOf(SecureCatalogImpl.class));

        assertThat(context.getBean("urlMasterPasswordProvider"), notNullValue());
    }
}
