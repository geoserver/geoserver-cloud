/*
 * (c) 2020 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.wfs.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.geoserver.cloud.wfs.security.NoopLayerGroupContainmentCache;
import org.geoserver.security.impl.LayerGroupContainmentCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.xmlunit.assertj3.XmlAssert;

import java.util.Map;

@SpringBootTest(classes = WfsApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
abstract class WfsApplicationTest {

    protected TestRestTemplate restTemplate = new TestRestTemplate("admin", "geoserver");

    @Autowired protected ApplicationContext appContext;

    @Test
    void owsGetCapabilitiesSmokeTest(@LocalServerPort int servicePort) {
        String url =
                "http://localhost:%d/ows?SERVICE=WFS&REQUEST=GETCAPABILITIES&VERSION=1.1.0"
                        .formatted(servicePort);
        String caps = restTemplate.getForObject(url, String.class);
        Map<String, String> nscontext = Map.of("wfs", "http://www.opengis.net/wfs");
        XmlAssert.assertThat(caps)
                .withNamespaceContext(nscontext)
                .hasXPath("/wfs:WFS_Capabilities");
    }

    @Test
    void wfsGetCapabilitiesSmokeTest(@LocalServerPort int servicePort) {
        String url =
                "http://localhost:%d/wfs?SERVICE=WFS&REQUEST=GETCAPABILITIES&VERSION=1.1.0"
                        .formatted(servicePort);
        String caps = restTemplate.getForObject(url, String.class);
        Map<String, String> nscontext = Map.of("wfs", "http://www.opengis.net/wfs");
        XmlAssert.assertThat(caps)
                .withNamespaceContext(nscontext)
                .hasXPath("/wfs:WFS_Capabilities");
    }

    @Test
    void noopLayerGroupContainmentCache() {
        var lgcc = appContext.getBean(LayerGroupContainmentCache.class);
        assertThat(lgcc).isInstanceOf(NoopLayerGroupContainmentCache.class);
    }
}
