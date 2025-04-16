/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cloud.autoconfigure.extensions.ogcapi.features;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.geoserver.cloud.autoconfigure.extensions.ConditionalOnGeoServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * {@link ConditionalOnProperty @ConditionalOnProperty} that checks if the OGC API Features
 * extension is enabled through the {@code geoserver.extension.ogcapi.features.enabled} property.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Inherited
@ConditionalOnGeoServer
@ConditionalOnClass(org.geoserver.ogcapi.v1.features.FeatureService.class)
@ConditionalOnProperty(
        prefix = OgcApiFeatureConfigProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = OgcApiFeatureConfigProperties.DEFAULT_ENABLED)
public @interface ConditionalOnOgcApiFeatures {}
