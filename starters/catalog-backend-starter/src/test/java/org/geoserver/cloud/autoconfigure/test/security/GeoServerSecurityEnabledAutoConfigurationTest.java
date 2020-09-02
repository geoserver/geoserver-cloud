/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.test.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.autoconfigure.security.GeoServerSecurityEnabledAutoConfiguration;
import org.geoserver.cloud.autoconfigure.testconfiguration.AutoConfigurationTestConfiguration;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/** Smoke test for {@link GeoServerSecurityEnabledAutoConfiguration} enabled */
@SpringBootTest(classes = AutoConfigurationTestConfiguration.class)
@EnableAutoConfiguration(
    exclude = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class}
)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
        "geoserver.backend.data-directory.enabled=true",
        "geoserver.security.enabled=true"
    }
)
public class GeoServerSecurityEnabledAutoConfigurationTest {

    private @Autowired @Qualifier("rawCatalog") Catalog rawCatalog;
    private @Autowired @Qualifier("secureCatalog") Catalog secureCatalog;

    private @Autowired ApplicationContext context;

    @Test
    public void secureCatalogIsSecureCatalogImpl() {
        assertThat(rawCatalog, instanceOf(org.geoserver.catalog.plugin.CatalogImpl.class));
        assertThat(secureCatalog, instanceOf(SecureCatalogImpl.class));
    }

    @Test
    public void accessRuleDAO() {
        assertNotNull(context.getBean(DataAccessRuleDAO.class));
    }

    @Test
    public void noAuthenticationManager() {
        assertNotNull(context.getBean(GeoServerSecurityManager.class));
    }
}
