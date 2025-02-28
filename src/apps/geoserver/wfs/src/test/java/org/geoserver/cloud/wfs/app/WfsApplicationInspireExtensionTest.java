/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.wfs.app;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = WfsApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"geoserver.extension.inspire.enabled=true"})
@ActiveProfiles("datadir")
class WfsApplicationInspireExtensionTest extends WfsApplicationDataDirectoryIT {

    @Test
    void owsGetCapabilitiesWithInspireSmokeTest(@LocalServerPort int servicePort) {
        String url = "http://localhost:%d/ows?SERVICE=WFS&REQUEST=GETCAPABILITIES&VERSION=1.1.0".formatted(servicePort);
        String caps = restTemplate.getForObject(url, String.class);

        Assertions.assertThat(caps)
                .contains("xmlns:inspire_dls=\"http://inspire.ec.europa.eu/schemas/inspire_dls/1.0\"");

        Assertions.assertThat(caps).contains("xmlns:inspire_common=\"http://inspire.ec.europa.eu/schemas/common/1.0\"");
    }

    @Test
    void wfsGetCapabilitiesWithInspireSmokeTest(@LocalServerPort int servicePort) {
        String url = "http://localhost:%d/wfs?SERVICE=WFS&REQUEST=GETCAPABILITIES&VERSION=1.1.0".formatted(servicePort);
        String caps = restTemplate.getForObject(url, String.class);

        Assertions.assertThat(caps)
                .contains("xmlns:inspire_dls=\"http://inspire.ec.europa.eu/schemas/inspire_dls/1.0\"");

        Assertions.assertThat(caps).contains("xmlns:inspire_common=\"http://inspire.ec.europa.eu/schemas/common/1.0\"");
    }

    @Test
    void testExpectedBeansFromWfsApplicationInspireExtensionAutoConfiguration() {
        expectBean("inspireWfsExtendedCapsProvider", org.geoserver.inspire.wfs.WFSExtendedCapabilitiesProvider.class);
        expectBean("languageCallback", org.geoserver.inspire.LanguagesDispatcherCallback.class);
        expectBean("inspireDirManager", org.geoserver.inspire.InspireDirectoryManager.class);
    }
}
