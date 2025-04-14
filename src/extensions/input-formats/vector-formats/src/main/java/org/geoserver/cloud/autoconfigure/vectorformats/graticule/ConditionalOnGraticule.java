/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.vectorformats.graticule;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.geotools.data.graticule.GraticuleDataStoreFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Composite annotation that combines conditions required for Graticule support.
 *
 * <p>
 * This conditional activates when:
 * <ul>
 * <li>The geoserver.extension.graticule.enabled property is true (the
 * default)</li>
 * </ul>
 *
 * <p>
 * This annotation can be used on configuration classes or bean methods to make
 * them conditional on Graticule being enabled.
 *
 * @see ConditionalOnProperty
 * @since 2.27.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
@ConditionalOnClass(GraticuleDataStoreFactory.class)
public @interface ConditionalOnGraticule {}
