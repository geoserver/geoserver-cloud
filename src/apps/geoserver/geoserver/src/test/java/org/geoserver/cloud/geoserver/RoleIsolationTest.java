/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.geoserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.geoserver.wms.WMS;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Boots a handful of role combinations to verify {@code geoserver.services} keeps unselected services out of the
 * context, rather than exercising every possible combination: the wms-only and webui-only roles cover the two isolation
 * directions that matter most, and the stateless-services composite covers running several non-webui services side by
 * side.
 *
 * <p>The web UI's admin pages for the wms, wfs, wcs and wps services (see {@code WebWmsAutoConfiguration} and its
 * siblings) each import that service's full core configuration to power their settings pages, independently of whether
 * the corresponding OWS service is in {@code geoserver.services}. A webui-only role therefore still answers OWS
 * requests for those four services; only the REST API and GWC, which have no such admin-page shortcut, are absent.
 */
class RoleIsolationTest {

    abstract static class WithDataDir {

        static @TempDir Path datadir;

        @DynamicPropertySource
        static void setUpDataDir(DynamicPropertyRegistry registry) {
            registry.add("geoserver.backend.data-directory.location", datadir::toAbsolutePath);
            registry.add("gwc.cache-directory", () -> datadir.resolve("gwc").toAbsolutePath());
        }
    }

    @Nested
    @SpringBootTest(classes = GeoServerApplication.class, properties = "geoserver.services=wms")
    @ActiveProfiles("test")
    class WmsRole extends WithDataDir {

        @Autowired
        ApplicationContext context;

        @Test
        void noWebUiNoRest() {
            assertThat(context.getBeansOfType(org.geoserver.web.GeoServerApplication.class))
                    .isEmpty();
            assertThat(context.containsBean("restRequestPathInfoFilter")).isFalse();
            assertThat(context.getBeansOfType(WMS.class)).isNotEmpty();
        }
    }

    @Nested
    @SpringBootTest(classes = GeoServerApplication.class, properties = "geoserver.services=webui")
    @ActiveProfiles("test")
    class WebUiRole extends WithDataDir {

        @Autowired
        ApplicationContext context;

        /**
         * The REST API and GWC, unlike wms/wfs/wcs/wps (see the class javadoc), have no web-ui admin-page shortcut that
         * pulls them in outside {@code geoserver.services}; a webui-only role must leave both off.
         */
        @Test
        void noRestNoGwcDispatch() {
            assertThat(context.getBeansOfType(org.geoserver.web.GeoServerApplication.class))
                    .isNotEmpty();
            assertThat(context.containsBean("restRequestPathInfoFilter")).isFalse();
            assertThat(context.containsBean("gwcDispatcherController")).isFalse();
        }
    }

    @Nested
    @SpringBootTest(
            classes = GeoServerApplication.class,
            properties = "geoserver.services=wms,wfs,wcs,wps,gwc,restconfig")
    @ActiveProfiles("test")
    class ServicesRole extends WithDataDir {

        @Autowired
        ApplicationContext context;

        @Test
        void statelessServicesWithoutWebUi() {
            assertThat(context.getBeansOfType(org.geoserver.web.GeoServerApplication.class))
                    .isEmpty();
            assertThat(context.getBeansOfType(WMS.class)).isNotEmpty();
            assertThat(context.containsBean("restRequestPathInfoFilter")).isTrue();
        }
    }
}
