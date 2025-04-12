/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * Annotation that marks a component to be conditional on the application being
 * a GeoServer WCS (Web Coverage Service) application.
 *
 * <p>
 * This annotation is used to selectively enable components that should only be active
 * in GeoServer WCS applications. It verifies that:
 * <ul>
 *   <li>The application has GeoServer core classes ({@link ConditionalOnGeoServer})</li>
 *   <li>The WCS service class is available in the classpath</li>
 *   <li>The WCS service bean (coverageResponseDelegateFactory) is registered in the application context</li>
 * </ul>
 *
 * <p>
 * Usage example:
 * <pre>{@code
 * @Configuration
 * @ConditionalOnGeoServerWCS
 * public class WcsSpecificConfiguration {
 *     // Configuration only activated in WCS service
 * }
 * }</pre>
 *
 * @see ConditionalOnGeoServer
 * @see ConditionalOnGeoServerWMS
 * @see ConditionalOnGeoServerWFS
 * @see ConditionalOnGeoServerWPS
 * @since 2.27.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ConditionalOnGeoServer
@ConditionalOnClass(org.geoserver.wcs.responses.CoverageResponseDelegateFinder.class)
@ConditionalOnBean(name = "coverageResponseDelegateFactory")
public @interface ConditionalOnGeoServerWCS {}
