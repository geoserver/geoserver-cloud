package org.geoserver.cloud.wcs;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = WcsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"geoserver.extension.inspire.enabled: true"})
@ActiveProfiles("test")
public class WcsApplicationInspireExtensionTest extends WcsApplicationTest {

    @Test
    void owsGetCapabilitiesWithInspireSmokeTest(@LocalServerPort int servicePort) {
        String url = "http://localhost:%d/ows?SERVICE=WCS&REQUEST=GETCAPABILITIES&VERSION=2.0.1".formatted(servicePort);
        String caps = restTemplate.getForObject(url, String.class);

        Assertions.assertThat(caps)
                .contains("xmlns:inspire_dls=\"http://inspire.ec.europa.eu/schemas/inspire_dls/1.0\"");

        Assertions.assertThat(caps).contains("xmlns:inspire_common=\"http://inspire.ec.europa.eu/schemas/common/1.0\"");
    }

    @Test
    void wfsGetCapabilitiesWithInspireSmokeTest(@LocalServerPort int servicePort) {
        String url = "http://localhost:%d/wcs?SERVICE=WCS&REQUEST=GETCAPABILITIES&VERSION=2.0.1".formatted(servicePort);
        String caps = restTemplate.getForObject(url, String.class);

        Assertions.assertThat(caps)
                .contains("xmlns:inspire_dls=\"http://inspire.ec.europa.eu/schemas/inspire_dls/1.0\"");

        Assertions.assertThat(caps).contains("xmlns:inspire_common=\"http://inspire.ec.europa.eu/schemas/common/1.0\"");
    }

    @Test
    void testExpectedBeansFromWcsApplicationInspireExtensionAutoConfiguration() {
        expectBean("inspireWcsExtendedCapsProvider", org.geoserver.inspire.wcs.WCSExtendedCapabilitiesProvider.class);
        expectBean("languageCallback", org.geoserver.inspire.LanguagesDispatcherCallback.class);
        expectBean("inspireDirManager", org.geoserver.inspire.InspireDirectoryManager.class);
    }
}
