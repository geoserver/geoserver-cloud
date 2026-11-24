/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

class GeoServerApplicationLauncherTest {

    private static final AtomicReference<Environment> ENVIRONMENT = new AtomicReference<>();

    @Configuration(proxyBeanMethods = false)
    static class TestApp {
        TestApp(Environment environment) {
            ENVIRONMENT.set(environment);
        }
    }

    // the profiles given to the launcher are added to the ones of the environment
    @Test
    void additionalProfiles() {
        GeoServerApplicationLauncher.run(
                TestApp.class,
                List.of("wms"),
                "--spring.main.web-application-type=none",
                "--spring.main.banner-mode=off",
                "--spring.profiles.active=local");

        assertThat(ENVIRONMENT.get().getActiveProfiles()).contains("wms", "local");
    }
}
