/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.event.bus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.bus.BusBridge;
import org.springframework.cloud.bus.ConditionalOnBusEnabled;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnMissingBean(BusBridge.class)
@ConditionalOnProperty(
        value = ConditionalOnBusEnabled.SPRING_CLOUD_BUS_ENABLED,
        matchIfMissing = false,
        havingValue = "false")
public @interface ConditionalOnGeoServerRemoteEventsDisabled {}
