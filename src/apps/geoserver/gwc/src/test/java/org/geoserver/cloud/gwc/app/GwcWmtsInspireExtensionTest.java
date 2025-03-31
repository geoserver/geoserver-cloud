package org.geoserver.cloud.gwc.app;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = GeoWebCacheApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"geoserver.extension.inspire.enabled: true"})
@ActiveProfiles("test")
public class GwcWmtsInspireExtensionTest extends GeoWebCacheApplicationTest {

    @Test
    void wmtsGetCapabilitiesWithInspireSmokeTest(@LocalServerPort int servicePort) {
        String url = "http://localhost:%d/gwc/service/wmts?SERVICE=WMTS&REQUEST=GETCAPABILITIES&VERSION=1.1.1"
                .formatted(servicePort);
        String caps = restTemplate.getForObject(url, String.class);

        Assertions.assertThat(caps)
                .contains("xmlns:inspire_vs=\"http://inspire.ec.europa.eu/schemas/inspire_vs_ows11/1.0\"");

        Assertions.assertThat(caps).contains("xmlns:inspire_common=\"http://inspire.ec.europa.eu/schemas/common/1.0\"");
    }

    @Test
    void testExpectedBeansFromGwcApplicationInspireExtensionAutoConfiguration() {
        expectBean(
                "inspireWmtsExtendedCapsProvider", org.geoserver.inspire.wmts.WMTSExtendedCapabilitiesProvider.class);
        expectBean("languageCallback", org.geoserver.inspire.LanguagesDispatcherCallback.class);
        expectBean("inspireGridSetLoader", org.geoserver.inspire.wmts.InspireGridSetLoader.class);
        expectBean("inspireDirManager", org.geoserver.inspire.InspireDirectoryManager.class);
    }
}
