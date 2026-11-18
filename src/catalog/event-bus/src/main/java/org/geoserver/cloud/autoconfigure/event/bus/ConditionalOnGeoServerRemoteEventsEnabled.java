/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.event.bus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.bus.ConditionalOnBusEnabled;

/**
 * GeoServer remote events enablement checks base on {@link ConditionalOnBusEnabled @ConditionalOnBusEnabled}
 * (spring-cloud-bus enabled) and configuration property {@code geoserver.bus.enabled=true} (defaults to {@code true}).
 *
 * <p>An extra {@code @ConditionalOnClass(name = "org.springframework.cloud.bus.BusBridge")} is added because the
 * dependency on {@code org.springframework.cloud:spring-cloud-bus} is optional.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnClass(name = "org.springframework.cloud.bus.BusBridge")
@ConditionalOnBusEnabled
@ConditionalOnProperty(name = "geoserver.bus.enabled", havingValue = "true", matchIfMissing = true)
public @interface ConditionalOnGeoServerRemoteEventsEnabled {}
