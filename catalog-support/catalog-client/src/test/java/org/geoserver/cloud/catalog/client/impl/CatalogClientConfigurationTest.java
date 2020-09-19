/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.impl;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = CatalogClientConfiguration.class)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class CatalogClientConfigurationTest {

    private @Autowired CloudCatalogFacade cloudCatalogFacade;

    @Test
    public void smokeTest() {
        assertNotNull(cloudCatalogFacade);
    }
}
