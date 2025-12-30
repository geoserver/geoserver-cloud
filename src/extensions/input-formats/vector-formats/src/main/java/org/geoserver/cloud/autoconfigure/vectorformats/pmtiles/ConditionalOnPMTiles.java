/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.cloud.autoconfigure.vectorformats.pmtiles;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.geotools.pmtiles.store.PMTilesDataStoreFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Composite annotation that combines conditions required for PMTiles support.
 *
 * <p>
 * This conditional activates when:
 * <ul>
 * <li>The {@code geoserver.extension.pmtiles.enabled} property is true (the default)
 * <li> The {@code org.geotools.pmtiles.store.PMTilesDataStoreFactory} class is in the classpath
 * </ul>
 *
 * <p>
 * This annotation can be used on configuration classes or bean methods to make
 * them conditional on PMTiles being enabled.
 *
 * @see ConditionalOnProperty
 * @since 2.28.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
@ConditionalOnProperty(name = "geoserver.extension.pmtiles.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(PMTilesDataStoreFactory.class)
public @interface ConditionalOnPMTiles {}
