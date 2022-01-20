/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.autoconfigure.test.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.geoserver.catalog.Catalog;
import org.geoserver.cloud.autoconfigure.security.GeoServerSecurityDisabledAutoConfiguration;
import org.geoserver.cloud.autoconfigure.testconfiguration.AutoConfigurationTestConfiguration;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.password.URLMasterPasswordProvider;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
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

/**
 * Smoke test to load the main context with auto-configuration enabled and without security as of
 * {@link GeoServerSecurityDisabledAutoConfiguration}
 */
@SpringBootTest(classes = AutoConfigurationTestConfiguration.class)
@EnableAutoConfiguration(
    exclude = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class}
)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
        "geoserver.backend.data-directory.enabled=true",
        "geoserver.security.enabled=false"
    }
)
public class GeoServerSecurityDisabledAutoConfigurationTest {

    private @Autowired @Qualifier("rawCatalog") Catalog rawCatalog;
    private @Autowired @Qualifier("secureCatalog") Catalog secureCatalog;

    private @Autowired ApplicationContext context;

    public @Test void secureCatalogIsRawCatalog() {
        Assume.assumeTrue(rawCatalog instanceof org.geoserver.catalog.plugin.CatalogPlugin);
        assertThat(rawCatalog, instanceOf(org.geoserver.catalog.plugin.CatalogPlugin.class));
        assertSame(secureCatalog, rawCatalog);
    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void accessRuleDAO() {
        assertNotNull(context.getBean(DataAccessRuleDAO.class));
    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void noUrlMasterPasswordProvider() {
        context.getBean(URLMasterPasswordProvider.class);
    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void noAuthenticationManager() {
        context.getBean(GeoServerSecurityManager.class);
    }
}
