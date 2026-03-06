/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.gateway;

import org.geoserver.cloud.app.GeoServerApplicationLauncher;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Spring Cloud Gateway application for GeoServer Cloud */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String... args) {
        GeoServerApplicationLauncher.run(GatewayApplication.class, args);
    }
}
