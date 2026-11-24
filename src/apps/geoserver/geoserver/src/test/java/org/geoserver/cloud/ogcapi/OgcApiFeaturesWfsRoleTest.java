/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.ogcapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.geoserver.cloud.geoserver.GeoServerApplication;
import org.geoserver.cloud.geoserver.SpringMvcOwnership;
import org.geoserver.ogcapi.APIConfigurationSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Verifies that the OGC API Features extension, which the WFS service activates, answers without taking Spring MVC over
 * from Spring Boot.
 *
 * <p>The extension contributes {@code APIConfigurationSupport}, a {@code WebMvcConfigurationSupport} subclass. It
 * reaches the context as a plain bean, never as a configuration class, and the OGC API dispatcher builds the handler
 * adapter it needs from it by hand. A service running neither the REST API nor GWC therefore keeps Spring Boot's
 * auto-configured Web MVC setup, exactly as it did before the OGC API extension existed.
 */
@SpringBootTest(
        classes = GeoServerApplication.class,
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"geoserver.services=wfs"})
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
class OgcApiFeaturesWfsRoleTest {

    static @TempDir Path datadir;

    @DynamicPropertySource
    static void setUpDataDir(DynamicPropertyRegistry registry) {
        registry.add("geoserver.backend.data-directory.location", datadir::toAbsolutePath);
        registry.add("gwc.cache-directory", () -> datadir.resolve("gwc").toAbsolutePath());
    }

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void springBootOwnsSpringMvc() {
        SpringMvcOwnership.assertSpringBootOwnsSpringMvc(context);
    }

    @Test
    void ogcApiSupportIsAPlainBeanAndNotAnMvcConfiguration() {
        String[] names = context.getBeanNamesForType(APIConfigurationSupport.class);
        assertThat(names).hasSize(1);

        BeanDefinition definition = context.getBeanFactory().getBeanDefinition(names[0]);
        assertThat(definition.getFactoryMethodName())
                .as("registered through a @Bean method, which Spring never treats as a configuration class")
                .isNotNull();
    }

    @Test
    void landingPageAnswersJson() {
        OgcApiFeaturesRequests.assertLandingPageAnswersJson(restTemplate);
    }

    @Test
    void conformanceAnswersJson() {
        OgcApiFeaturesRequests.assertConformanceAnswersJson(restTemplate);
    }
}
