/*
 * (c) 2025 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver.cloud.wms.app;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = WmsApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
            "geoserver.extension.inspire.enabled=true",
            "geoserver.backend.data-directory.enabled=true",
            "gwc.wms-integration=true"
        })
@ActiveProfiles({"test"})
class WmsApplicationInspireExtensionTest extends WmsApplicationDataDirectoryTest {

    @Test
    void getCapabilitiesInspireExtensionSmokeTest() {
        String url = baseURL + "?SERVICE=WMS&REQUEST=GETCAPABILITIES&VERSION=1.3.0";
        String caps = restTemplate.getForObject(url, String.class);

        Assertions.assertThat(caps).contains("xmlns:inspire_vs=\"http://inspire.ec.europa.eu/schemas/inspire_vs/1.0\"");

        Assertions.assertThat(caps).contains("xmlns:inspire_common=\"http://inspire.ec.europa.eu/schemas/common/1.0\"");
    }

    @Test
    void testExpectedBeansFromWmsApplicationInspireExtensionAutoConfiguration() {
        expectBean("inspireWmsExtendedCapsProvider", org.geoserver.inspire.wms.WMSExtendedCapabilitiesProvider.class);
        expectBean("languageCallback", org.geoserver.inspire.LanguagesDispatcherCallback.class);
        expectBean("inspireDirManager", org.geoserver.inspire.InspireDirectoryManager.class);
    }
}
