/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.bus.catalog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.bus.ConditionalOnBusEnabled;

@Slf4j
@ConditionalOnProperty(
    value = ConditionalOnBusEnabled.SPRING_CLOUD_BUS_ENABLED,
    matchIfMissing = false,
    havingValue = "false"
)
public class BusDisabledLogger {

    public BusDisabledLogger() {
        log.warn("GeoServer Catalog and configuration event-bus is disabled");
    }
}
