/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.metrics.catalog;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Groups conditions that shall be met to enable geoserver metrics
 *
 * <ul>
 *   <li>Both spring-boot-actuator and micrometer shall be available;
 *   <li>A {@link MeterRegistry} bean shall be available as per {@link MetricsAutoConfiguration}
 *   <li>{@literal geoserver.metrics.enabled} is unset or {@code true}
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnClass({MeterRegistry.class, Timed.class})
@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnProperty(name = "geoserver.metrics.enabled", havingValue = "true", matchIfMissing = true)
public @interface ConditionalOnGeoServerMetricsEnabled {}
