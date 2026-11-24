/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.web.app;

import java.util.List;
import org.geoserver.cloud.app.GeoServerApplicationLauncher;
import org.geoserver.cloud.geoserver.GeoServerApplication;

/**
 * Runs {@link GeoServerApplication} as the web UI service.
 *
 * <p>The {@value #SERVICE} profile loads {@code application-webui.yml}, with the name, the role and the base path of
 * the service.
 */
public class WebUIApplication {

    /** The name of the service and of the Spring profile with its settings */
    public static final String SERVICE = "webui";

    private WebUIApplication() {
        // launcher
    }

    public static void main(String... args) {
        GeoServerApplicationLauncher.run(GeoServerApplication.class, List.of(SERVICE), args);
    }
}
