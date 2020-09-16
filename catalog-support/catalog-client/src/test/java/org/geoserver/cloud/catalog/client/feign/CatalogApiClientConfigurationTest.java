/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.catalog.client.feign;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = {CatalogApiClientConfiguration.class})
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@ActiveProfiles("test")
public class CatalogApiClientConfigurationTest {

    private @Autowired WorkspaceClient workspaceClient;
    private @Autowired NamespaceClient namespaceClient;
    private @Autowired StoreClient storeClient;
    private @Autowired ResourceClient resourceClient;
    private @Autowired LayerClient layerClient;
    private @Autowired LayerGroupClient layerGroupClient;
    private @Autowired StyleClient styleClient;
    private @Autowired MapClient mapClient;

    @Test
    public void smokeTest() {
        assertNotNull(workspaceClient);
        assertNotNull(namespaceClient);
        assertNotNull(storeClient);
        assertNotNull(resourceClient);
        assertNotNull(layerClient);
        assertNotNull(layerGroupClient);
        assertNotNull(styleClient);
        assertNotNull(mapClient);
    }
}
