/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.config.main;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.security.SecureCatalogImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/** Smoke test to load the main context without auto-configuration enabled and without security */
@RunWith(SpringRunner.class)
@EnableAutoConfiguration(
    exclude = { //
        DataSourceAutoConfiguration.class, //
        DataSourceTransactionManagerAutoConfiguration.class, //
        HibernateJpaAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        UserDetailsServiceAutoConfiguration.class, //
        ManagementWebSecurityAutoConfiguration.class
    }
)
@SpringBootTest(classes = {GeoServerMainConfiguration.class})
@ActiveProfiles("test")
public class GeoServerMainConfigurationSmokeTest {

    private @Autowired @Qualifier("rawCatalog") Catalog rawCatalog;
    private @Autowired @Qualifier("secureCatalog") Catalog secureCatalog;

    public @Test void contextLoads() {
        assertThat(rawCatalog, instanceOf(CatalogImpl.class));
        assertThat(secureCatalog, instanceOf(SecureCatalogImpl.class));
    }
}
