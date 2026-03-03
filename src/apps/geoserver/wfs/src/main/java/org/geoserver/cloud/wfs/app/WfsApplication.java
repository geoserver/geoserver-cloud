/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.wfs.app;

import org.geoserver.cloud.app.GeoServerApplicationLauncher;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class WfsApplication {

    public static void main(String... args) {
        GeoServerApplicationLauncher.run(WfsApplication.class, args);
    }
}
