/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gwc.app;

import java.util.List;
import org.geoserver.cloud.app.GeoServerApplicationLauncher;
import org.geoserver.cloud.geoserver.GeoServerApplication;

/**
 * Runs {@link GeoServerApplication} as the GeoWebCache service.
 *
 * <p>The {@value #SERVICE} profile loads {@code application-gwc.yml}, with the name, the role and the base path of the
 * service.
 */
public class GeoWebCacheApplication {

    /** The name of the service and of the Spring profile with its settings */
    public static final String SERVICE = "gwc";

    private GeoWebCacheApplication() {
        // launcher
    }

    public static void main(String... args) {
        GeoServerApplicationLauncher.run(GeoServerApplication.class, List.of(SERVICE), args);
    }
}
