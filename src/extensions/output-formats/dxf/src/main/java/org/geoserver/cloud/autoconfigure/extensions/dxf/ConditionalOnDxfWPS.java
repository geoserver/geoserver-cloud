/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.extensions.dxf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Composite annotation that combines conditions required for DXF WPS extension support
 * across multiple GeoServer services.
 *
 * <p>
 * This conditional activates when:
 * <ul>
 *   <li>All conditions from {@link ConditionalOnDxf}</li>
 *   <li>The geoserver.extension.dxf.wps property is true (the default)</li>
 * </ul>
 * @since 2.28.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
@ConditionalOnDxf
@ConditionalOnProperty(name = "geoserver.extension.dxf.wps", havingValue = "true", matchIfMissing = true)
public @interface ConditionalOnDxfWPS {}
